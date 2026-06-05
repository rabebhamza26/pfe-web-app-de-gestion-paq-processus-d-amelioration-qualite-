package com.polytech.paqbackend.service;

import com.polytech.paqbackend.controller.PaqController;
import com.polytech.paqbackend.dto.EntretienFinalDTO;
import com.polytech.paqbackend.entity.EntretienFinal;
import com.polytech.paqbackend.entity.PaqDossier;
import com.polytech.paqbackend.repository.CollaboratorRepository;
import com.polytech.paqbackend.repository.EntretienFinalRepository;
import com.polytech.paqbackend.repository.PaqRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class EntretienFinalService {

    private static final Logger log = LoggerFactory.getLogger(EntretienFinalService.class);

    private final EntretienFinalRepository entretienFinalRepository;
    private final PaqRepository paqRepository;
    private final NotificationService notificationService;
    private final CollaboratorRepository collaboratorRepository;
    private final ObjectMapper objectMapper;

    public EntretienFinalService(EntretienFinalRepository entretienFinalRepository,
                                 PaqRepository paqRepository,
                                 NotificationService notificationService,
                                 CollaboratorRepository collaboratorRepository) {
        this.entretienFinalRepository = entretienFinalRepository;
        this.paqRepository = paqRepository;
        this.notificationService = notificationService;
        this.collaboratorRepository = collaboratorRepository;
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

    public void deleteAvecNotification(Long id, String matricule, String expediteurEmail, String destinataireEmail, String nomCollab) {
        EntretienFinal entretien = entretienFinalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entretien introuvable: " + id));

        entretienFinalRepository.deleteById(id);

        Optional<PaqDossier> paqOpt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);
        if (paqOpt.isPresent()) {
            PaqDossier paq = paqOpt.get();

            String historique = addHistorique(
                    paq.getHistorique(),
                    new PaqController.HistoriqueEvent(
                            LocalDate.now(),
                            "SUPPRESSION ENTRETIEN FINAL",
                            String.format("Entretien final supprimé le %s", LocalDate.now())
                    )
            );
            paq.setHistorique(historique);
            paqRepository.save(paq);
        }

        // Envoyer une notification de suppression
        String nomCollaborateur = getCollaborateurNom(matricule);
        notificationService.envoyerNotification(
                expediteurEmail,
                "Entretien final supprimé",
                String.format("L'entretien final du collaborateur %s (matricule: %s) a été supprimé.", nomCollaborateur, matricule),
                "WARNING"
        );

        log.info("Entretien final {} supprimé pour {}", id, matricule);
    }

    public EntretienFinal create(String matricule, EntretienFinalDTO dto, String expediteurEmail) {
        PaqDossier dossier = paqRepository
                .findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule)
                .orElseThrow(() -> new RuntimeException("Dossier PAQ actif non trouvé pour le matricule : " + matricule));

        // Permettre l'entretien final si niveau 4 ou 5
        if (dossier.getNiveau() < 4 || dossier.getNiveau() > 5) {
            throw new RuntimeException("Le niveau actuel (" + dossier.getNiveau() + ") ne permet pas l'entretien final (niveau 4 ou 5 requis)");
        }

        EntretienFinal entretien = new EntretienFinal();
        entretien.setMatricule(matricule);
        entretien.setDecision(dto.getDecision());
        entretien.setDateEntretien(dto.getDateEntretien() != null ? dto.getDateEntretien() : LocalDate.now());
        entretien.setTypeFaute(dto.getTypeFaute());
        entretien.setCommentaireRH(dto.getCommentaireRH());
        entretien.setCasca(dto.getCasca());

        EntretienFinal saved = entretienFinalRepository.save(entretien);

        LocalDate dateReelle = entretien.getDateEntretien();

        if (dossier.getNiveau() == 4) {
            dossier.setDateCinquiemeEntretien(dateReelle);
            dossier.setNiveau(5);
            dossier.setStatut("CLOTURE");
        }

        dossier.setCinquiemeEntretienNotes(dto.getCommentaireRH() != null ? dto.getCommentaireRH() : "");

        String historique = addHistorique(dossier.getHistorique(),
                new PaqController.HistoriqueEvent(dateReelle,
                        "ENTRETIEN FINAL",
                        String.format("Entretien final créé le %s — Décision : %s — Faute : %s",
                                dateReelle, dto.getDecision(), dto.getTypeFaute())));
        dossier.setHistorique(historique);
        paqRepository.save(dossier);

        // Envoyer une notification de création
        String nomCollaborateur = getCollaborateurNom(matricule);
        notificationService.envoyerNotification(
                expediteurEmail,
                "Entretien final créé",
                String.format("L'entretien final du collaborateur %s (matricule: %s) a été créé avec succès. Décision: %s",
                        nomCollaborateur, matricule, dto.getDecision()),
                "SUCCESS"
        );

        log.info("Entretien final créé pour le matricule {} par {}", matricule, expediteurEmail);
        return saved;
    }

    public EntretienFinal updateWithPaqUpdate(Long id, String matricule, EntretienFinalDTO dto, String expediteurEmail) {
        EntretienFinal existing = entretienFinalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entretien introuvable: " + id));

        existing.setDecision(dto.getDecision());
        existing.setDateEntretien(dto.getDateEntretien());
        existing.setTypeFaute(dto.getTypeFaute());
        existing.setCommentaireRH(dto.getCommentaireRH());
        existing.setCasca(dto.getCasca());

        EntretienFinal updated = entretienFinalRepository.save(existing);

        Optional<PaqDossier> paqOpt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);
        if (paqOpt.isPresent()) {
            PaqDossier paq = paqOpt.get();
            paq.setCinquiemeEntretienNotes(dto.getCommentaireRH() != null ? dto.getCommentaireRH() : "");

            String historique = addHistorique(
                    paq.getHistorique(),
                    new PaqController.HistoriqueEvent(
                            LocalDate.now(),
                            "MODIFICATION ENTRETIEN FINAL",
                            String.format("Entretien final modifié le %s", LocalDate.now())
                    )
            );
            paq.setHistorique(historique);
            paqRepository.save(paq);
        }

        // Envoyer une notification de modification
        String nomCollaborateur = getCollaborateurNom(matricule);
        notificationService.envoyerNotification(
                expediteurEmail,
                "Entretien final modifié",
                String.format("L'entretien final du collaborateur %s (matricule: %s) a été modifié.",
                        nomCollaborateur, matricule),
                "INFO"
        );

        log.info("Entretien final {} modifié pour le matricule {} par {}", id, matricule, expediteurEmail);
        return updated;
    }

    public List<EntretienFinal> getByMatricule(String matricule) {
        return entretienFinalRepository.findByMatricule(matricule);
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

    public EntretienFinal findById(Long id) {
        return entretienFinalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entretien introuvable: " + id));
    }
}