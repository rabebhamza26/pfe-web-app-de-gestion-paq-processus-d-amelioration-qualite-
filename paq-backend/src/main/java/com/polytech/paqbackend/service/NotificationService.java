package com.polytech.paqbackend.service;

import com.polytech.paqbackend.entity.Collaborator;
import com.polytech.paqbackend.entity.Notification;
import com.polytech.paqbackend.entity.User;
import com.polytech.paqbackend.repository.CollaboratorRepository;
import com.polytech.paqbackend.repository.NotificationRepository;
import com.polytech.paqbackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repo;
    private final UserRepository userRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository repo,
                               UserRepository userRepository,
                               CollaboratorRepository collaboratorRepository,
                               SimpMessagingTemplate messagingTemplate) {
        this.repo = repo;
        this.userRepository = userRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.messagingTemplate = messagingTemplate;
    }

    private String resolveLoginFromPrincipal(String principal) {
        if (principal == null || principal.isBlank()) return null;

        User user = userRepository.findByEmailOrLogin(principal);
        if (user != null && user.getLogin() != null) {
            return user.getLogin();
        }
        return principal;
    }

    @Transactional
    public Notification envoyerNotification(String loginDestinataire,
                                            String titre,
                                            String message,
                                            String type,
                                            String matriculeCollaborateur,
                                            String typeEntretien) {

        String loginCanonique = resolveLoginFromPrincipal(loginDestinataire);
        if (loginCanonique == null) {
            log.error("Impossible de résoudre le login pour : {}", loginDestinataire);
            return null;
        }

        User destinataire = userRepository.findByLogin(loginCanonique);
        if (destinataire == null) {
            log.error("Utilisateur non trouvé pour le login : {}", loginCanonique);
            return null;
        }

        // Récupérer le nom du collaborateur si matriculeCollaborateur est fourni
        String nomCollaborateur = "";
        if (matriculeCollaborateur != null && !matriculeCollaborateur.isBlank()) {
            Optional<Collaborator> collaboratorOpt = collaboratorRepository.findByMatricule(matriculeCollaborateur);
            if (collaboratorOpt.isPresent()) {
                Collaborator collab = collaboratorOpt.get();
                nomCollaborateur = collab.getName() + " " + collab.getPrenom();
            }
        }

        try {
            Notification notif = new Notification();
            notif.setDestinataireLogin(loginCanonique);
            notif.setDestinataireEmail(destinataire.getEmail());
            notif.setTitre(titre);
            notif.setMessage(message);
            notif.setType(type != null ? type : "INFO");
            notif.setLu(false);
            notif.setCreatedAt(LocalDateTime.now());
            notif.setMatriculeCollaborateur(matriculeCollaborateur);
            notif.setTypeEntretien(typeEntretien);

            Notification saved = repo.save(notif);
            log.info("Notification sauvegardée pour login={}", loginCanonique);

            // Envoi WebSocket
            Map<String, Object> notificationDTO = convertToDTO(saved, nomCollaborateur);

            messagingTemplate.convertAndSendToUser(
                    loginCanonique,
                    "/queue/notifications",
                    notificationDTO
            );

            log.info("Notification WebSocket envoyée à user: {}", loginCanonique);

            return saved;

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification : {}", e.getMessage(), e);
            return null;
        }
    }

    public Notification envoyerNotification(String destinataireLogin,
                                            String titre,
                                            String message,
                                            String type) {
        return envoyerNotification(destinataireLogin, titre, message, type, null, null);
    }

    public List<Map<String, Object>> getNotificationsByLogin(String login) {
        List<Notification> notifications = repo.findByDestinataireLoginOrderByCreatedAtDesc(login);
        return notifications.stream()
                .map(n -> convertToDTO(n, getCollaboratorName(n.getMatriculeCollaborateur())))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getUnreadNotificationsByLogin(String login) {
        List<Notification> notifications = repo.findByDestinataireLoginAndLuOrderByCreatedAtDesc(login, false);
        return notifications.stream()
                .map(n -> convertToDTO(n, getCollaboratorName(n.getMatriculeCollaborateur())))
                .collect(Collectors.toList());
    }

    public long countUnreadByLogin(String login) {
        return repo.countByDestinataireLoginAndLu(login, false);
    }

    @Transactional
    public void markAsRead(Long id, String login) {
        Optional<Notification> notifOpt = repo.findById(id);
        if (notifOpt.isEmpty()) {
            log.warn("markAsRead : notification {} introuvable", id);
            return;
        }

        Notification notif = notifOpt.get();

        if (!login.equals(notif.getDestinataireLogin())) {
            log.warn("markAsRead refusé : notif {} appartient à '{}', demandé par '{}'",
                    id, notif.getDestinataireLogin(), login);
            throw new SecurityException("Accès non autorisé à cette notification");
        }

        repo.markAsRead(id);
    }

    @Transactional
    public int markAllAsRead(String login) {
        repo.markAllAsRead(login);
        return 0;
    }

    private String getCollaboratorName(String matriculeCollaborateur) {
        if (matriculeCollaborateur == null || matriculeCollaborateur.isBlank()) return "";
        Optional<Collaborator> collaborator = collaboratorRepository.findByMatricule(matriculeCollaborateur);
        if (collaborator.isPresent()) {
            Collaborator c = collaborator.get();
            return c.getName() + " " + c.getPrenom();
        }
        return matriculeCollaborateur;
    }

    private Map<String, Object> convertToDTO(Notification n, String nomCollaborateur) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", n.getId());
        dto.put("destinataireLogin", n.getDestinataireLogin());
        dto.put("titre", n.getTitre());
        dto.put("message", n.getMessage());
        dto.put("lu", n.isLu());
        dto.put("createdAt", n.getCreatedAt() != null ?
                n.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        dto.put("type", n.getType());
        dto.put("typeEntretien", n.getTypeEntretien());
        dto.put("matriculeCollaborateur", n.getMatriculeCollaborateur());
        dto.put("nomCollaborateur", nomCollaborateur);
        return dto;
    }
}