package com.polytech.paqbackend.service;

import com.polytech.paqbackend.controller.PaqController;
import com.polytech.paqbackend.dto.EntretienDecisionRequestDTO;
import com.polytech.paqbackend.entity.EntretienDecision;
import com.polytech.paqbackend.entity.PaqDossier;
import com.polytech.paqbackend.repository.CollaboratorRepository;
import com.polytech.paqbackend.repository.EntretienDecisionRepository;
import com.polytech.paqbackend.repository.PaqRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class EntretienDecisionService {

    private static final Logger log = LoggerFactory.getLogger(EntretienDecisionService.class);

    private final EntretienDecisionRepository repo;
    private final PaqRepository paqRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final UserService userService;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public EntretienDecisionService(EntretienDecisionRepository repo,
                                    PaqRepository paqRepository,
                                    CollaboratorRepository collaboratorRepository,
                                    UserService userService,
                                    EmailService emailService,
                                    NotificationService notificationService) {
        this.repo = repo;
        this.paqRepository = paqRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.userService = userService;
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    private String addHistorique(String historiqueJson, PaqController.HistoriqueEvent event) {
        try {
            List<PaqController.HistoriqueEvent> list;
            if (historiqueJson == null || historiqueJson.isBlank() || "[]".equals(historiqueJson)) {
                list = new ArrayList<>();
            } else {
                list = objectMapper.readValue(historiqueJson,
                        new TypeReference<List<PaqController.HistoriqueEvent>>() {});
            }
            list.add(event);
            list.sort(Comparator.comparing(PaqController.HistoriqueEvent::getDate));
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return String.format("[{\"date\":\"%s\",\"action\":\"%s\",\"detail\":\"%s\"}]",
                    event.getDate(), event.getAction(), event.getDetail());
        }
    }

    private String getCollaborateurNom(String matricule) {
        try {
            return collaboratorRepository.findByMatricule(matricule)
                    .map(c -> c.getName() + " " + c.getPrenom())
                    .orElse(matricule);
        } catch (Exception e) {
            return matricule;
        }
    }

    // ✅ Envoi d'emails aux destinataires
// ✅ Envoi d'emails aux destinataires - Version avec tous les paramètres
    private void envoyerEmailsSL(List<String> destinataires, String messageOptionnel, String matricule,
                                 String typeAction, String typeFaute, LocalDate dateEntretien, String justification) {
        if (destinataires == null || destinataires.isEmpty()) {
            log.warn("Aucun destinataire spécifié pour l'envoi d'email");
            return;
        }

        String nomCollab = getCollaborateurNom(matricule);
        String expediteur = "noreply@leoni.com";

        log.info("=== ENVOI EMAILS ===");
        log.info("Destinataires: {}", destinataires);
        log.info("Type action: {}", typeAction);
        log.info("Type faute: {}", typeFaute);
        log.info("Date entretien: {}", dateEntretien);
        log.info("Justification: {}", justification);
        log.info("Message optionnel: {}", messageOptionnel);

        for (String destinataire : destinataires) {
            if (destinataire != null && !destinataire.trim().isEmpty()) {
                try {
                    String sujet = String.format("[PAQ] Entretien de décision - %s - %s", nomCollab, typeAction);
                    String htmlContent = buildEmailContent(nomCollab, matricule, typeAction, messageOptionnel,
                            typeFaute, dateEntretien, justification);
                    emailService.sendEmail(expediteur, destinataire, sujet, htmlContent);
                    log.info("✅ Email envoyé avec succès à: {}", destinataire);
                } catch (Exception e) {
                    log.error("❌ Erreur envoi email à {}: {}", destinataire, e.getMessage(), e);
                }
            }
        }
    }    private String buildEmailContent(String nomCollab, String matricule, String typeAction, String messageOptionnel,
                                     String typeFaute, LocalDate dateEntretien, String justification) {
        String actionTexte = "";
        switch (typeAction) {
            case "CRÉATION": actionTexte = "créé"; break;
            case "MODIFICATION": actionTexte = "modifié"; break;
            case "VALIDATION SL": actionTexte = "soumis pour validation"; break;
            default: actionTexte = typeAction;
        }

        // Formater la date
        DateTimeFormatter frenchFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String dateFormatted = dateEntretien != null ? dateEntretien.format(frenchFormatter) : "Non définie";

        // Construction du message optionnel
        String messageHtml = "";
        if (messageOptionnel != null && !messageOptionnel.isEmpty()) {
            messageHtml = String.format("""
            <tr>
              <td style="padding:8px;border:1px solid #ddd;"><strong>Message</strong></td>
              <td style="padding:8px;border:1px solid #ddd;">%s</td>
            </tr>
            """, messageOptionnel);
        }

        // Ajouter la cause principale si présente
        String justificationHtml = "";
        if (justification != null && !justification.isEmpty()) {
            justificationHtml = String.format("""
            <tr>
              <td style="padding:8px;border:1px solid #ddd;"><strong>Cause principale</strong></td>
              <td style="padding:8px;border:1px solid #ddd;">%s</td>
            </tr>
            """, justification);
        }

        return String.format("""
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"></head>
        <body style="font-family: Arial, sans-serif;">
          <div style="max-width:600px;margin:auto;background:white;border-radius:8px;padding:20px;box-shadow:0 2px 8px rgba(0,0,0,.1);">
            <div style="background:#C8102E;padding:15px;border-radius:8px 8px 0 0;margin:-20px -20px 0 -20px;">
              <h2 style="color:white;margin:0;"> PAQ - Entretien de Décision</h2>
            </div>
            <div style="padding:20px 0;">
              <p>Bonjour,</p>
              <p><strong>Merci d'assister à l'entretien de décision</strong></p>
              <p>Un entretien de décision a été <strong>%s</strong> pour le collaborateur :</p>
              <table style="width:100%%;border-collapse:collapse;margin:20px 0;">
                <tr style="background:#f8f9fa;">
                  <td style="padding:10px;border:1px solid #ddd;font-weight:600;width:40%%">Collaborateur</td>
                  <td style="padding:10px;border:1px solid #ddd;"><strong>%s</strong></td>
                </tr>
                <tr style="background:#ffffff;">
                  <td style="padding:10px;border:1px solid #ddd;font-weight:600;">Matricule</td>
                  <td style="padding:10px;border:1px solid #ddd;"><code>%s</code></td>
                </tr>
                <tr style="background:#f8f9fa;">
                  <td style="padding:10px;border:1px solid #ddd;font-weight:600;">Type de faute</td>
                  <td style="padding:10px;border:1px solid #ddd;"><strong>%s</strong></td>
                </tr>
                <tr style="background:#ffffff;">
                  <td style="padding:10px;border:1px solid #ddd;font-weight:600;">Date entretien</td>
                  <td style="padding:10px;border:1px solid #ddd;">%s</td>
                </tr>
                %s
                %s
               </table>
              <div style="background:#e3f2fd;padding:15px;border-radius:8px;margin:15px 0;">
                <p style="margin:0;color:#1565c0;">
                  <strong>🔔 Action requise :</strong><br>
                  Connectez-vous à la plateforme PAQ pour consulter et valider cet entretien.
                </p>
              </div>
            </div>
            <div style="background:#f8f9fa;padding:10px;text-align:center;font-size:11px;color:#888;border-radius:0 0 8px 8px;margin:0 -20px -20px -20px;">
              Email automatique - Système PAQ LEONI
            </div>
          </div>
        </body>
        </html>
        """,
                actionTexte,              // %s 1 - "modifié", "créé", etc.
                nomCollab,                // %s 2 - Nom du collaborateur
                matricule,               // %s 3 - Matricule
                typeFaute != null ? typeFaute : "",  // %s 4 - Type de faute
                dateFormatted,           // %s 5 - Date formatée (dd/MM/yyyy)
                justificationHtml,       // %s 6 - Cause principale
                messageHtml              // %s 7 - Message optionnel
        );
    }
    // ─── CRÉATION (SL) ─────────────────────────────────────────────────────────
    public EntretienDecision create(String matricule, EntretienDecisionRequestDTO dto, String expediteurEmail) {
        Optional<PaqDossier> paqOpt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);
        if (paqOpt.isEmpty()) {
            throw new RuntimeException("Aucun dossier PAQ actif trouvé pour le matricule : " + matricule);
        }

        PaqDossier paq = paqOpt.get();

        if (paq.getNiveau() != 3) {
            throw new RuntimeException("Le niveau actuel (" + paq.getNiveau() + ") ne permet pas l'entretien de décision (niveau 3 requis)");
        }

        EntretienDecision entretien = new EntretienDecision();
        entretien.setMatricule(matricule);
        entretien.setTypeFaute(dto.getTypeFaute());
        entretien.setDateEntretien(dto.getDateEntretien() != null ? dto.getDateEntretien() : LocalDate.now());
        entretien.setDecision(dto.getDecision());
        entretien.setJustification(dto.getJustification());
        entretien.setDateCreation(LocalDate.now());
        entretien.setValideSL(false);
        entretien.setValideHPSGL(false);
        entretien.setValideQMPlant(false);
        entretien.setCasca(dto.getCasca());


        EntretienDecision saved = repo.save(entretien);
        log.info("Entretien décision créé avec ID: {}", saved.getId());

        paq.setDateQuatriemeEntretien(entretien.getDateEntretien());
        paq.setQuatriemeEntretienNotes(dto.getDecision());

        String historique = addHistorique(paq.getHistorique(),
                new PaqController.HistoriqueEvent(entretien.getDateEntretien(),
                        "ENTRETIEN DE DÉCISION",
                        String.format("Entretien de décision créé le %s — Décision : %s",
                                entretien.getDateEntretien(), dto.getDecision()))
        );
        paq.setHistorique(historique);
        paqRepository.save(paq);

        return saved;
    }

    public EntretienDecision createAvecNotification(String matricule, EntretienDecisionRequestDTO dto, String expediteurEmail) {
        EntretienDecision saved = create(matricule, dto, expediteurEmail);
        if (dto.getDestinatairesEmails() != null && !dto.getDestinatairesEmails().isEmpty()) {
            envoyerEmailsSL(dto.getDestinatairesEmails(), dto.getMessageOptionnel(), matricule,
                    "CRÉATION", dto.getTypeFaute(), dto.getDateEntretien(), dto.getJustification());
        }
        return saved;
    }
    // ─── MODIFICATION (SL) ──────────────────────────────────────────────────────
    public EntretienDecision updateAvecNotification(Long id, String matricule, EntretienDecisionRequestDTO dto, String expediteurEmail) {
        EntretienDecision updated = updateWithPaqUpdate(id, matricule, dto);
        if (dto.getDestinatairesEmails() != null && !dto.getDestinatairesEmails().isEmpty()) {
            envoyerEmailsSL(dto.getDestinatairesEmails(), dto.getMessageOptionnel(), matricule,
                    "MODIFICATION", dto.getTypeFaute(), dto.getDateEntretien(), dto.getJustification());
        }
        return updated;
    }
    private EntretienDecision updateWithPaqUpdate(Long id, String matricule, EntretienDecisionRequestDTO dto) {
        EntretienDecision existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Entretien introuvable: " + id));

        if (dto.getTypeFaute() != null) existing.setTypeFaute(dto.getTypeFaute());
        if (dto.getDateEntretien() != null) existing.setDateEntretien(dto.getDateEntretien());
        if (dto.getDecision() != null) existing.setDecision(dto.getDecision());
        if (dto.getJustification() != null) existing.setJustification(dto.getJustification());

        EntretienDecision updated = repo.save(existing);

        Optional<PaqDossier> paqOpt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);
        if (paqOpt.isPresent()) {
            PaqDossier paq = paqOpt.get();
            paq.setQuatriemeEntretienNotes(dto.getDecision());
            String historique = addHistorique(paq.getHistorique(),
                    new PaqController.HistoriqueEvent(LocalDate.now(),
                            "MODIFICATION ENTRETIEN DE DÉCISION",
                            String.format("Entretien de décision modifié le %s", LocalDate.now()))
            );
            paq.setHistorique(historique);
            paqRepository.save(paq);
        }

        return updated;
    }

    // ✅ VALIDATION SL (avec envoi d'emails)
// ✅ VALIDATION SL (avec envoi d'emails)
    public EntretienDecision validerParSL(Long id, String matricule, EntretienDecisionRequestDTO dto, String expediteurEmail) {
        log.info("=== validerParSL START ===");
        log.info("ID reçu: {}", id);
        log.info("DTO reçu - TypeFaute: {}, Decision: {}", dto.getTypeFaute(), dto.getDecision());
        log.info("DestinatairesEmails dans DTO: {}", dto.getDestinatairesEmails());
        log.info("Size destinataires: {}", dto.getDestinatairesEmails() != null ? dto.getDestinatairesEmails().size() : 0);

        EntretienDecision existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Entretien introuvable: " + id));

        // Mettre à jour les données
        if (dto.getTypeFaute() != null) existing.setTypeFaute(dto.getTypeFaute());
        if (dto.getDateEntretien() != null) existing.setDateEntretien(dto.getDateEntretien());
        if (dto.getDecision() != null) existing.setDecision(dto.getDecision());
        if (dto.getJustification() != null) existing.setJustification(dto.getJustification());

        existing.setValideSL(true);
        EntretienDecision updated = repo.save(existing);
        log.info("Entretien mis à jour - valideSL = {}", updated.isValideSL());

        // Mise à jour historique PAQ
        Optional<PaqDossier> paqOpt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);
        if (paqOpt.isPresent()) {
            PaqDossier paq = paqOpt.get();
            String historique = addHistorique(paq.getHistorique(),
                    new PaqController.HistoriqueEvent(LocalDate.now(),
                            "VALIDATION SL ENTRETIEN DE DÉCISION",
                            String.format("Entretien de décision validé par SL le %s ", LocalDate.now()))
            );
            paq.setHistorique(historique);
            paqRepository.save(paq);
        }

        // ✅ Envoi des emails aux destinataires sélectionnés
        List<String> destinatairesSelectionnes = dto.getDestinatairesEmails();
        log.info("Destinataires à envoyer: {}", destinatairesSelectionnes);

        if (destinatairesSelectionnes != null && !destinatairesSelectionnes.isEmpty()) {
            log.info("Envoi des emails à {} destinataires", destinatairesSelectionnes.size());
            envoyerEmailsSL(destinatairesSelectionnes, dto.getMessageOptionnel(), matricule,
                    "VALIDATION SL", dto.getTypeFaute(), dto.getDateEntretien(), dto.getJustification());
        }

        return updated;
    }
    // ✅ VALIDATION HP/SGL (1ère validation) - sans email
    public EntretienDecision validerParHPSGL(Long id, String matricule, EntretienDecisionRequestDTO dto, String expediteurEmail) {
        log.info("=== validerParHPSGL START ===");

        EntretienDecision existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Entretien introuvable: " + id));

        if (dto.getTypeFaute() != null) existing.setTypeFaute(dto.getTypeFaute());
        if (dto.getDateEntretien() != null) existing.setDateEntretien(dto.getDateEntretien());
        if (dto.getDecision() != null) existing.setDecision(dto.getDecision());
        if (dto.getJustification() != null) existing.setJustification(dto.getJustification());

        existing.setValideHPSGL(true);
        EntretienDecision updated = repo.save(existing);
        log.info("Entretien mis à jour - valideHPSGL = {}", updated.isValideHPSGL());

        Optional<PaqDossier> paqOpt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);
        if (paqOpt.isPresent()) {
            PaqDossier paq = paqOpt.get();
            String historique = addHistorique(paq.getHistorique(),
                    new PaqController.HistoriqueEvent(LocalDate.now(),
                            "PREMIÈRE VALIDATION ENTRETIEN DE DÉCISION",
                            String.format("Entretien de décision validé par %s le %s ", expediteurEmail, LocalDate.now()))
            );
            paq.setHistorique(historique);
            paqRepository.save(paq);
        }

        return updated;
    }

    // ✅ VALIDATION QM_PLANT (2ème validation) - sans email
    public EntretienDecision validerParQMPlant(Long id, String matricule, EntretienDecisionRequestDTO dto, String expediteurEmail) {
        log.info("=== validerParQMPlant START ===");

        EntretienDecision existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Entretien introuvable: " + id));

        if (!existing.isValideHPSGL()) {
            throw new RuntimeException("La validation HP/SGL est requise avant la validation QM_PLANT");
        }

        if (dto.getTypeFaute() != null) existing.setTypeFaute(dto.getTypeFaute());
        if (dto.getDateEntretien() != null) existing.setDateEntretien(dto.getDateEntretien());
        if (dto.getDecision() != null) existing.setDecision(dto.getDecision());
        if (dto.getJustification() != null) existing.setJustification(dto.getJustification());

        existing.setValideQMPlant(true);
        EntretienDecision updated = repo.save(existing);
        log.info("Entretien mis à jour - valideQMPlant = {}", updated.isValideQMPlant());

        Optional<PaqDossier> paqOpt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);
        if (paqOpt.isPresent()) {
            PaqDossier paq = paqOpt.get();

            // ✅ Passage au niveau 4 (Entretien Final)
            if (paq.getNiveau() == 3) {
                paq.setNiveau(4);
                paq.setDateCinquiemeEntretien(LocalDate.now());

                String historique = addHistorique(paq.getHistorique(),
                        new PaqController.HistoriqueEvent(LocalDate.now(),
                                "DEUXIÈME VALIDATION ENTRETIEN DE DÉCISION",
                                String.format("Entretien de décision validé par QM_PLANT le %s ", LocalDate.now()))
                );
                paq.setHistorique(historique);
                paqRepository.save(paq);

                log.info("PAQ niveau 3 → 4 après validation QM_PLANT pour {}", matricule);
            }
        }

        return updated;
    }

    public void deleteAvecNotification(Long id, String matricule, String expediteurEmail, String destinataireEmail, String nomCollab) {
        repo.deleteById(id);
        log.info("Entretien de décision {} supprimé", id);
    }

    public List<EntretienDecision> findByMatricule(String matricule) {
        return repo.findByMatricule(matricule);
    }

    public EntretienDecision findById(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Entretien introuvable id=" + id));
    }
}