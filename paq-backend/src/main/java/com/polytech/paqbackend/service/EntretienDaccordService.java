package com.polytech.paqbackend.service;

import com.polytech.paqbackend.controller.PaqController;
import com.polytech.paqbackend.dto.EntretienDaccordRequestDTO;
import com.polytech.paqbackend.entity.EntretienDaccord;
import com.polytech.paqbackend.entity.PaqDossier;
import com.polytech.paqbackend.repository.CollaboratorRepository;
import com.polytech.paqbackend.repository.EntretienDaccordRepository;
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
public class EntretienDaccordService {

    private static final Logger log = LoggerFactory.getLogger(EntretienDaccordService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EntretienDaccordRepository repo;
    private final PaqRepository paqRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    public EntretienDaccordService(EntretienDaccordRepository repo,
                                   PaqRepository paqRepository,
                                   CollaboratorRepository collaboratorRepository,
                                   EmailService emailService) {
        this.repo = repo;
        this.paqRepository = paqRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.emailService = emailService;
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
            log.error("Erreur mise à jour historique", e);
            return String.format("[{\"date\":\"%s\",\"action\":\"%s\",\"detail\":\"%s\"}]",
                    event.getDate(), event.getAction(), event.getDetail());
        }
    }

    // ─── CRÉATION ────────────────────────────────────────────────────────────────
    // SL crée l'entretien → PAQ passe au niveau 2 (niveau Entretien d'Accord)

    public EntretienDaccord create(String matricule, EntretienDaccordRequestDTO dto, String expediteurEmail) {
        Optional<PaqDossier> paqOpt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);
        if (paqOpt.isEmpty()) {
            throw new RuntimeException("Aucun dossier PAQ actif trouvé pour le matricule : " + matricule);
        }


        PaqDossier paq = paqOpt.get();

        if (paq.getNiveau() != 1) {
            throw new RuntimeException("Le niveau actuel (" + paq.getNiveau() + ") ne permet pas l'entretien d'accord (niveau 1 requis)");
        }

        EntretienDaccord e = new EntretienDaccord();
        e.setMatricule(matricule);
        e.setDate(dto.getDate() != null ? dto.getDate() : LocalDate.now());
        e.setTypeFaute(dto.getTypeFaute());
        e.setCauseFaute(dto.getCauseFaute());
        e.setMesuresProposees(dto.getMesuresProposees() != null ? dto.getMesuresProposees() : "");
        e.setValide(false);
        e.setCasca(dto.getCasca()); // NOUVEAU


        EntretienDaccord saved = repo.save(e);

        paq.setNiveau(2);
        paq.setDateDeuxiemeEntretien(e.getDate());
        paq.setDeuxiemeEntretienNotes(dto.getMesuresProposees());

        String dateFormatted = e.getDate().format(DATE_FORMATTER);
        String historique = addHistorique(paq.getHistorique(),
                new PaqController.HistoriqueEvent(e.getDate(),
                        "ENTRETIEN D'ACCORD",
                        String.format("Entretien d'accord créé le %s — Faute : %s",
                                dateFormatted, dto.getTypeFaute()))
        );
        paq.setHistorique(historique);
        paqRepository.save(paq);

        log.info("Historique mis à jour avec ENTRETIEN D'ACCORD pour le matricule {}", matricule);

        return saved;
    }

    public EntretienDaccord createAvecNotification(String matricule, EntretienDaccordRequestDTO dto, String expediteurEmail) {
        return create(matricule, dto, expediteurEmail);
    }

    // ─── MODIFICATION ────────────────────────────────────────────────────────────

    private EntretienDaccord updateWithPaqUpdate(Long id, String matricule, EntretienDaccordRequestDTO dto, String expediteurEmail) {
        EntretienDaccord existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Entretien introuvable: " + id));

        if (dto.getDate() != null) existing.setDate(dto.getDate());
        if (dto.getTypeFaute() != null) existing.setTypeFaute(dto.getTypeFaute());
        if (dto.getCauseFaute() != null) existing.setCauseFaute(dto.getCauseFaute());
        if (dto.getMesuresProposees() != null) existing.setMesuresProposees(dto.getMesuresProposees());

        if (dto.getCasca() != null) existing.setCasca(dto.getCasca()); // NOUVEAU

        EntretienDaccord updated = repo.save(existing);

        Optional<PaqDossier> paqOpt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);
        if (paqOpt.isPresent()) {
            PaqDossier paq = paqOpt.get();
            paq.setDeuxiemeEntretienNotes(dto.getMesuresProposees());
            String dateFormatted = LocalDate.now().format(DATE_FORMATTER);
            String historique = addHistorique(paq.getHistorique(),
                    new PaqController.HistoriqueEvent(LocalDate.now(),
                            "MODIFICATION ENTRETIEN D'ACCORD",
                            String.format("Entretien d'accord modifié le %s par %s", dateFormatted, expediteurEmail))
            );
            paq.setHistorique(historique);
            paqRepository.save(paq);
        }

        return updated;
    }

    public EntretienDaccord updateAvecNotification(Long id, String matricule, EntretienDaccordRequestDTO dto, String expediteurEmail) {
        EntretienDaccord updated = updateWithPaqUpdate(id, matricule, dto, expediteurEmail);
        log.info("Entretien d'accord modifié pour {}", matricule);
        return updated;
    }

    // ─── VALIDATION SL ───────────────────────────────────────────────────────────
    // SL soumet → valide=true + email QM + historique PAQ
    // Le niveau du PAQ ne change PAS ici (reste 2)

    public EntretienDaccord validerPremiere(Long id, String matricule, EntretienDaccordRequestDTO dto, String expediteurEmail) {
        EntretienDaccord existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Entretien introuvable: " + id));

        String nomCollab = getCollaborateurNom(matricule);
        String destinataireEmail = dto.getDestinataireEmail();

        Optional<PaqDossier> paqOpt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);
        if (paqOpt.isPresent()) {
            PaqDossier paq = paqOpt.get();
            boolean alreadyDone = false;
            if (paq.getHistorique() != null && !paq.getHistorique().isEmpty()) {
                try {
                    List<PaqController.HistoriqueEvent> hist = objectMapper.readValue(
                            paq.getHistorique(), new TypeReference<List<PaqController.HistoriqueEvent>>() {});
                    alreadyDone = hist.stream().anyMatch(ev -> "VALIDATION SL ENTRETIEN D'ACCORD".equals(ev.getAction()));
                } catch (Exception ex) {
                    log.warn("Erreur vérification historique", ex);
                }
            }
            if (!alreadyDone) {
                String dateFormatted = LocalDate.now().format(DATE_FORMATTER);
                String historique = addHistorique(paq.getHistorique(),
                        new PaqController.HistoriqueEvent(LocalDate.now(),
                                "VALIDATION SL ENTRETIEN D'ACCORD",
                                String.format("Entretien d'accord soumis par SL le %s ", dateFormatted))
                );
                paq.setHistorique(historique);
                paqRepository.save(paq);
            }
        }

        existing.setValide(true);
        existing = repo.save(existing);

        if (destinataireEmail != null && !destinataireEmail.isBlank()) {
            envoyerEmailConvocation(expediteurEmail, destinataireEmail, nomCollab, matricule, dto);
            log.info("Email de convocation envoyé à {} pour {}", destinataireEmail, matricule);
        } else {
            log.warn("Aucun email QM_SEGMENT fourni pour {}", matricule);
        }

        return existing;
    }

    // ─── VALIDATION QM_SEGMENT ───────────────────────────────────────────────────
    public EntretienDaccord validerFinale(Long id, String matricule, EntretienDaccordRequestDTO dto, String expediteurEmail) {
        EntretienDaccord existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Entretien introuvable: " + id));

        // Mise à jour des champs si fournis
        if (dto.getDate() != null) existing.setDate(dto.getDate());
        if (dto.getTypeFaute() != null) existing.setTypeFaute(dto.getTypeFaute());
        if (dto.getCauseFaute() != null) existing.setCauseFaute(dto.getCauseFaute());
        if (dto.getMesuresProposees() != null) existing.setMesuresProposees(dto.getMesuresProposees());
        if (dto.getCasca() != null) existing.setCasca(dto.getCasca()); // NOUVEAU

        // Marquer validé par QM
        existing.setValide(true);
        existing.setValideQM(true);
        EntretienDaccord updated = repo.save(existing);

        log.info("Entretien d'accord {} validé par QM-Segment pour {}", id, matricule);

        // Mise à jour du dossier PAQ
        Optional<PaqDossier> paqOpt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);
        if (paqOpt.isPresent()) {
            PaqDossier paq = paqOpt.get();

            log.info("=== DEBUG: Niveau PAQ AVANT validation: {} ===", paq.getNiveau());

            // Mise à jour des notes mesures
            if (dto.getMesuresProposees() != null) {
                paq.setDeuxiemeEntretienNotes(dto.getMesuresProposees());
            }

            // ✅ IMPORTANT: NE PAS CHANGER LE NIVEAU !
            // Le niveau reste à 2 car l'entretien d'accord est validé
            // C'est l'entretien de mesure qui passera le niveau à 3
            // Donc on ne fait PAS paq.setNiveau()

            String dateFormatted = LocalDate.now().format(DATE_FORMATTER);
            String historique = addHistorique(paq.getHistorique(),
                    new PaqController.HistoriqueEvent(LocalDate.now(),
                            "VALIDATION QM-SEGMENT ENTRETIEN D'ACCORD",
                            String.format("Entretien d'accord validé par QM-Segment le %s", dateFormatted))
            );
            paq.setHistorique(historique);
            paqRepository.save(paq);

            log.info("✅ PAQ mis à jour - niveau maintenu à {} pour {}", paq.getNiveau(), matricule);
        } else {
            log.warn("Aucun dossier PAQ actif trouvé pour {} lors de la validation QM-Segment", matricule);
        }

        return updated;
    }
    public EntretienDaccord findById(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Entretien introuvable id=" + id));
    }

    public List<EntretienDaccord> findByMatricule(String matricule) {
        return repo.findByMatricule(matricule);
    }

    public void delete(Long id) {
        repo.deleteById(id);
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

    private void envoyerEmailConvocation(String expediteur, String destinataire,
                                         String nomCollab, String matricule,
                                         EntretienDaccordRequestDTO dto) {
        try {
            String sujet = String.format("[PAQ] Entretien d'accord  - %s", nomCollab);
            String htmlContent = buildEmailConvocationContent(nomCollab, matricule, dto);
            emailService.sendEmail(expediteur, destinataire, sujet, htmlContent);
            log.info("Email  envoyé à {} pour {}", destinataire, matricule);
        } catch (Exception e) {
            log.error("Erreur envoi email à {}: {}", destinataire, e.getMessage());
        }
    }

    private String buildEmailConvocationContent(String nomCollab, String matricule, EntretienDaccordRequestDTO dto) {
        return String.format("""
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"></head>
        <body style="font-family: Arial, sans-serif;">
          <div style="max-width:600px;margin:auto;background:white;border-radius:8px;padding:20px;">
            <div style="background:#C8102E;padding:15px;border-radius:8px 8px 0 0;margin:-20px -20px 0 -20px;">
              <h2 style="color:white;margin:0;">PAQ - Entretien d'accord</h2>
            </div>
            <div style="padding:20px 0;">
              <p>Bonjour,</p>
              <p><strong>Merci d'assister à l'entretien d'accord</strong></p>
              <table style="width:100%%;border-collapse:collapse;margin:20px 0;">
                <tr><td style="padding:8px;border:1px solid #ddd;"><strong>Collaborateur</strong></td>
                    <td style="padding:8px;border:1px solid #ddd;">%s</td></tr>
                <tr><td style="padding:8px;border:1px solid #ddd;"><strong>Matricule</strong></td>
                    <td style="padding:8px;border:1px solid #ddd;">%s</td></tr>
                <tr><td style="padding:8px;border:1px solid #ddd;"><strong>Type de faute</strong></td>
                    <td style="padding:8px;border:1px solid #ddd;">%s</td></tr>
                <tr><td style="padding:8px;border:1px solid #ddd;"><strong>Date</strong></td>
                    <td style="padding:8px;border:1px solid #ddd;">%s</td></tr>
                <tr><td style="padding:8px;border:1px solid #ddd;"><strong>Cause de faute</strong></td>
                    <td style="padding:8px;border:1px solid #ddd;">%s</td></tr>
                <tr><td style="padding:8px;border:1px solid #ddd;"><strong>Mesures proposées</strong></td>
                    <td style="padding:8px;border:1px solid #ddd;">%s</td></tr>
              </table>
              <p>Veuillez vous connecter au système PAQ pour valider cet entretien.</p>
            </div>
          </div>
        </body>
        </html>
        """, nomCollab, matricule,
                dto.getTypeFaute() != null ? dto.getTypeFaute() : "",
                dto.getDate() != null ? dto.getDate().toString() : "",
                dto.getCauseFaute() != null ? dto.getCauseFaute() : "",
                dto.getMesuresProposees() != null ? dto.getMesuresProposees() : "");
    }
}