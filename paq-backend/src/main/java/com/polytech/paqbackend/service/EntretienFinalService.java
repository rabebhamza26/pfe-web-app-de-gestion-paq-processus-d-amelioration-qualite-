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
    private final EmailService emailService;
    private final CollaboratorRepository collaboratorRepository;
    private final ObjectMapper objectMapper;

    public EntretienFinalService(EntretienFinalRepository entretienFinalRepository,
                                 PaqRepository paqRepository,
                                 NotificationService notificationService,
                                 EmailService emailService,
                                 CollaboratorRepository collaboratorRepository) {
        this.entretienFinalRepository = entretienFinalRepository;
        this.paqRepository = paqRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
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

    public EntretienFinal createAvecNotification(String matricule, EntretienFinalDTO dto, String expediteurEmail) {
        EntretienFinal saved = create(matricule, dto, expediteurEmail);

        String nomCollab = getCollaborateurNom(matricule);
        String destinataireEmail = dto.getDestinataireEmail();

        if (destinataireEmail != null && !destinataireEmail.isBlank()) {
            envoyerEmailValidation(expediteurEmail, destinataireEmail, nomCollab, matricule, dto, "créé");
            notificationService.envoyerNotification(
                    expediteurEmail,
                    "📧 Email envoyé",
                    "Un email concernant l'entretien final de " + nomCollab + " a été envoyé à " + destinataireEmail,
                    "SUCCESS", matricule, "FINAL"
            );
        } else {
            log.warn("Aucun email destinataire fourni pour l'entretien final de {}", matricule);
        }

        return saved;
    }

    public EntretienFinal updateAvecNotification(Long id,
                                                 String matricule,
                                                 EntretienFinalDTO dto,
                                                 String expediteurEmail) {
        EntretienFinal updated = updateWithPaqUpdate(id, matricule, dto);

        String nomCollab = getCollaborateurNom(matricule);
        String destinataireEmail = dto.getDestinataireEmail();

        if (destinataireEmail != null && !destinataireEmail.isBlank()) {
            envoyerEmailValidation(expediteurEmail, destinataireEmail, nomCollab, matricule, dto, "modifié");
            notificationService.envoyerNotification(
                    expediteurEmail,
                    "📧 Email envoyé",
                    "Un email concernant la modification de l'entretien final de " + nomCollab + " a été envoyé à " + destinataireEmail,
                    "SUCCESS", matricule, "FINAL"
            );
        }

        return updated;
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
                            " SUPPRESSION ENTRETIEN FINAL",
                            String.format("Entretien final supprimé le %s", LocalDate.now())
                    )
            );
            paq.setHistorique(historique);
            paqRepository.save(paq);
        }

        if (destinataireEmail != null && !destinataireEmail.isBlank()) {
            envoyerEmailSuppression(expediteurEmail, destinataireEmail, nomCollab, matricule);
        }

        log.info("Entretien final {} supprimé pour {}", id, matricule);
    }

    public EntretienFinal create(String matricule, EntretienFinalDTO dto, String expediteurEmail) {
        PaqDossier dossier = paqRepository
                .findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule)
                .orElseThrow(() -> new RuntimeException("Dossier PAQ actif non trouvé pour le matricule : " + matricule));

        // Permettre l'entretien final si niveau 4 ou 5 (modification)
        if (dossier.getNiveau() < 4 || dossier.getNiveau() > 5) {
            throw new RuntimeException("Le niveau actuel (" + dossier.getNiveau() + ") ne permet pas l'entretien final (niveau 4 ou 5 requis)");
        }

        EntretienFinal entretien = new EntretienFinal();
        entretien.setMatricule(matricule);
        entretien.setDecision(dto.getDecision());
        entretien.setDateEntretien(dto.getDateEntretien() != null ? dto.getDateEntretien() : LocalDate.now());
        entretien.setTypeFaute(dto.getTypeFaute());
        entretien.setCommentaireRH(dto.getCommentaireRH());

        entretien.setCasca(dto.getCasca()); // NOUVEAU


        EntretienFinal saved = entretienFinalRepository.save(entretien);

        // Ne mettre à jour le niveau que si c'est un nouvel entretien (niveau 4)
        LocalDate dateReelle = entretien.getDateEntretien();

        if (dossier.getNiveau() == 4) {
            dossier.setDateCinquiemeEntretien(dateReelle);
            dossier.setNiveau(5);
            dossier.setStatut("CLOTURE");
        }

        dossier.setCinquiemeEntretienNotes(dto.getCommentaireRH() != null ? dto.getCommentaireRH() : "");

        String historique = addHistorique(dossier.getHistorique(),
                new PaqController.HistoriqueEvent(dateReelle,
                        " ENTRETIEN FINAL",
                        String.format("Entretien final %s le %s — Décision : %s — Faute : %s",
                                saved.getId() != null ? "validé" : "modifié",
                                dateReelle, dto.getDecision(), dto.getTypeFaute())));
        dossier.setHistorique(historique);
        paqRepository.save(dossier);

        return saved;
    }

    public EntretienFinal updateWithPaqUpdate(Long id, String matricule, EntretienFinalDTO dto) {
        EntretienFinal existing = entretienFinalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entretien introuvable: " + id));

        existing.setDecision(dto.getDecision());
        existing.setDateEntretien(dto.getDateEntretien());
        existing.setTypeFaute(dto.getTypeFaute());
        existing.setCommentaireRH(dto.getCommentaireRH());
        existing.setCasca(dto.getCasca()); // NOUVEAU


        EntretienFinal updated = entretienFinalRepository.save(existing);

        Optional<PaqDossier> paqOpt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);
        if (paqOpt.isPresent()) {
            PaqDossier paq = paqOpt.get();
            paq.setCinquiemeEntretienNotes(dto.getCommentaireRH() != null ? dto.getCommentaireRH() : "");

            String historique = addHistorique(
                    paq.getHistorique(),
                    new PaqController.HistoriqueEvent(
                            LocalDate.now(),
                            " MODIFICATION ENTRETIEN FINAL",
                            String.format("Entretien final modifié le %s", LocalDate.now())
                    )
            );
            paq.setHistorique(historique);
            paqRepository.save(paq);
        }

        return updated;
    }

    public List<EntretienFinal> getByMatricule(String matricule) {
        return entretienFinalRepository.findByMatricule(matricule);
    }

    public void delete(Long id) {
        entretienFinalRepository.deleteById(id);
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

    private void envoyerEmailValidation(String expediteur, String destinataire,
                                        String nomCollab, String matricule,
                                        EntretienFinalDTO dto, String action) {
        try {
            String sujet = String.format("[PAQ] Entretien final %s - %s", action, nomCollab);
            String htmlContent = buildEmailContent(nomCollab, matricule, dto, action);
            emailService.sendEmail(expediteur, destinataire, sujet, htmlContent);
            log.info("Email envoyé à {} pour l'entretien final {} de {}", destinataire, action, matricule);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email à {}: {}", destinataire, e.getMessage());
        }
    }

    private void envoyerEmailSuppression(String expediteur, String destinataire,
                                         String nomCollab, String matricule) {
        try {
            String sujet = String.format("[PAQ] Entretien final supprimé - %s", nomCollab);
            String htmlContent = buildEmailSuppressionContent(nomCollab, matricule);
            emailService.sendEmail(expediteur, destinataire, sujet, htmlContent);
            log.info("Email de suppression envoyé à {} pour {}", destinataire, matricule);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de suppression à {}: {}", destinataire, e.getMessage());
        }
    }

    private String buildEmailContent(String nomCollab, String matricule,
                                     EntretienFinalDTO dto, String action) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif;">
              <div style="max-width:600px;margin:auto;background:white;border-radius:8px;padding:20px;">
                <div style="background:#C8102E;padding:15px;border-radius:8px 8px 0 0;margin:-20px -20px 0 -20px;">
                  <h2 style="color:white;margin:0;">🏭 PAQ - Entretien final %s - Dossier clôturé</h2>
                </div>
                <div style="padding:20px 0;">
                  <p>Bonjour,</p>
                  <p>L'entretien final a été <strong>%s</strong> pour le collaborateur :</p>
                  <table style="width:100%%;border-collapse:collapse;margin:20px 0;">
                    <tr><td style="padding:8px;border:1px solid #ddd;"><strong>Collaborateur</strong></td>
                        <td style="padding:8px;border:1px solid #ddd;">%s</td>
                    </tr>
                    <tr><td style="padding:8px;border:1px solid #ddd;"><strong>Matricule</strong></td>
                        <td style="padding:8px;border:1px solid #ddd;">%s</td>
                    </tr>
                    <tr><td style="padding:8px;border:1px solid #ddd;"><strong>Type de faute</strong></td>
                        <td style="padding:8px;border:1px solid #ddd;">%s</td>
                    </tr>
                    <tr><td style="padding:8px;border:1px solid #ddd;"><strong>Date entretien</strong></td>
                        <td style="padding:8px;border:1px solid #ddd;">%s</td>
                    </tr>
                    <tr><td style="padding:8px;border:1px solid #ddd;"><strong>Décision RH</strong></td>
                        <td style="padding:8px;border:1px solid #ddd;">%s</td>
                    </tr>
                    <tr><td style="padding:8px;border:1px solid #ddd;"><strong>Commentaire RH</strong></td>
                        <td style="padding:8px;border:1px solid #ddd;">%s</td>
                    </tr>
                  </table>
                  <p style="color: #C8102E;">Le dossier PAQ est désormais <strong>CLÔTURÉ</strong>.</p>
                </div>
              </div>
            </body>
            </html>
            """, action.equals("créé") ? "Validé" : "Modifié",
                action, nomCollab, matricule, dto.getTypeFaute(), dto.getDateEntretien(),
                dto.getDecision(), dto.getCommentaireRH() != null ? dto.getCommentaireRH() : "");
    }

    private String buildEmailSuppressionContent(String nomCollab, String matricule) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif;">
              <div style="max-width:600px;margin:auto;background:white;border-radius:8px;padding:20px;">
                <div style="background:#C8102E;padding:15px;border-radius:8px 8px 0 0;margin:-20px -20px 0 -20px;">
                  <h2 style="color:white;margin:0;">🏭 PAQ - Suppression d'entretien final</h2>
                </div>
                <div style="padding:20px 0;">
                  <p>Bonjour,</p>
                  <p>L'entretien final pour le collaborateur suivant a été supprimé :</p>
                  <table style="width:100%%;border-collapse:collapse;margin:20px 0;">
                    <tr><td style="padding:8px;border:1px solid #ddd;"><strong>Collaborateur</strong></td>
                        <td style="padding:8px;border:1px solid #ddd;">%s</td>
                    </tr>
                    <tr><td style="padding:8px;border:1px solid #ddd;"><strong>Matricule</strong></td>
                        <td style="padding:8px;border:1px solid #ddd;">%s</td>
                    </tr>
                  </table>
                  <p style="color: #C8102E;">L'entretien a été supprimé du système.</p>
                </div>
              </div>
            </body>
            </html>
            """, nomCollab, matricule);
    }

    public EntretienFinal findById(Long id) {
        return entretienFinalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entretien introuvable: " + id));
    }


}