package com.polytech.paqbackend.service;

import com.polytech.paqbackend.controller.PaqController;
import com.polytech.paqbackend.dto.EntretienMesureRequestDTO;
import com.polytech.paqbackend.entity.EntretienMesure;
import com.polytech.paqbackend.entity.PaqDossier;
import com.polytech.paqbackend.repository.CollaboratorRepository;
import com.polytech.paqbackend.repository.EntretienMesureRepository;
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
public class EntretienMesureService {

    private static final Logger log = LoggerFactory.getLogger(EntretienMesureService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EntretienMesureRepository repo;
    private final PaqRepository paqRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final CollaboratorRepository collaboratorRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public EntretienMesureService(EntretienMesureRepository repo,
                                  PaqRepository paqRepository,
                                  NotificationService notificationService,
                                  EmailService emailService,
                                  CollaboratorRepository collaboratorRepository,
                                  UserService userService) {
        this.repo = repo;
        this.paqRepository = paqRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.collaboratorRepository = collaboratorRepository;
        this.userService = userService;
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
    // ─── CRÉATION ────────────────────────────────────────────────────────────────
    // ─── CRÉATION ────────────────────────────────────────────────────────────────
    public EntretienMesure create(String matricule, EntretienMesureRequestDTO dto, String expediteurEmail) {
        log.info("=== create START ===");

        LocalDate dateEntretien = dto.getDateEntretien() != null ? dto.getDateEntretien() : LocalDate.now();

        Optional<PaqDossier> paqOpt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);
        if (paqOpt.isEmpty()) {
            throw new RuntimeException("Aucun dossier PAQ actif trouvé pour le matricule : " + matricule);
        }

        PaqDossier paq = paqOpt.get();

        if (paq.getNiveau() != 2) {
            throw new RuntimeException("Le niveau actuel (" + paq.getNiveau() + ") ne permet pas l'entretien de mesure (niveau 2 requis)");
        }

        EntretienMesure e = new EntretienMesure();
        e.setMatricule(matricule);
        e.setTypeFaute(dto.getTypeFaute());
        e.setCausesPrincipales(dto.getCausesPrincipales());
        e.setConvention(dto.getConvention());
        e.setPlanAction(dto.getPlanAction());
        e.setDateEntretien(dateEntretien);
        e.setCasca(dto.getCasca());
        e.setDateCreation(LocalDate.now());
        e.setValideSL(false);  // ⚠️ Important: false au départ
        e.setValideQM(false);
        e.setValideSGL(false);

        EntretienMesure saved = repo.save(e);
        log.info("Entretien mesure créé avec ID: {}, valideSL: {}", saved.getId(), saved.isValideSL());

        // Mise à jour du PAQ
        paq.setDateTroisiemeEntretien(dateEntretien);
        String notes = "Type faute: " + dto.getTypeFaute();
        paq.setTroisiemeEntretienNotes(notes);

        String dateFormatted = dateEntretien.format(DATE_FORMATTER);
        String historique = addHistorique(paq.getHistorique(),
                new PaqController.HistoriqueEvent(dateEntretien,
                        "ENTRETIEN DE MESURE",
                        String.format("Entretien de mesure créé le %s", dateFormatted))
        );
        paq.setHistorique(historique);
        paqRepository.save(paq);

        return saved;
    }
    public EntretienMesure createAvecNotification(String matricule, EntretienMesureRequestDTO dto, String expediteurEmail) {
        log.info("=== createAvecNotification START ===");
        EntretienMesure saved = create(matricule, dto, expediteurEmail);
        log.info("=== createAvecNotification END ===");
        return saved;
    }

    // ─── MODIFICATION ────────────────────────────────────────────────────────────
    public EntretienMesure updateAvecNotification(Long id, String matricule, EntretienMesureRequestDTO dto, String expediteurEmail) {
        EntretienMesure existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Entretien introuvable: " + id));


        existing.setTypeFaute(dto.getTypeFaute());
        existing.setCausesPrincipales(dto.getCausesPrincipales());
        existing.setConvention(dto.getConvention());
        existing.setPlanAction(dto.getPlanAction());
        existing.setDateEntretien(dto.getDateEntretien());
        existing.setCasca(dto.getCasca()); // ✅ AJOUT DU CHAMP CASCA




        EntretienMesure updated = repo.save(existing);

        Optional<PaqDossier> paqOpt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);
        if (paqOpt.isPresent()) {
            PaqDossier paq = paqOpt.get();
            String notes = "Type faute: " + dto.getTypeFaute()
                    + " | Plan action: " + (dto.getPlanAction() != null ? dto.getPlanAction() : "");
            paq.setTroisiemeEntretienNotes(notes);

            String dateFormatted = LocalDate.now().format(DATE_FORMATTER);
            String historique = addHistorique(paq.getHistorique(),
                    new PaqController.HistoriqueEvent(LocalDate.now(),
                            "MODIFICATION ENTRETIEN DE MESURE",
                            String.format("Entretien de mesure modifié le %s par %s", dateFormatted, expediteurEmail))
            );
            paq.setHistorique(historique);
            paqRepository.save(paq);
        }

        return updated;
    }

    public EntretienMesure update(Long id, EntretienMesureRequestDTO dto) {
        EntretienMesure e = repo.findById(id).orElseThrow();
        e.setTypeFaute(dto.getTypeFaute());
        e.setCausesPrincipales(dto.getCausesPrincipales());
        e.setConvention(dto.getConvention());
        e.setPlanAction(dto.getPlanAction());
        e.setDateEntretien(dto.getDateEntretien());
        e.setCasca(dto.getCasca()); // ✅ AJOUT DU CHAMP CASCA

        return repo.save(e);
    }

    // ─── VALIDATION SL (avec envoi d'emails aux destinataires sélectionnés) ───────
// ─── VALIDATION SL ───────────────────────────────────────────────────────────
// ─── VALIDATION SL (avec envoi d'emails aux destinataires sélectionnés) ───────
    public EntretienMesure validerPremiere(Long id, String matricule,
                                           EntretienMesureRequestDTO dto,
                                           String expediteurEmail) {
        log.info("=== validerPremiere START ===");
        log.info("ID: {}, Matricule: {}", id, matricule);

        EntretienMesure existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Entretien introuvable: " + id));

        String nomCollab = getCollaborateurNom(matricule);

        // Mettre à jour les données si fournies
        if (dto.getTypeFaute() != null) existing.setTypeFaute(dto.getTypeFaute());
        if (dto.getCausesPrincipales() != null) existing.setCausesPrincipales(dto.getCausesPrincipales());
        if (dto.getConvention() != null) existing.setConvention(dto.getConvention());
        if (dto.getPlanAction() != null) existing.setPlanAction(dto.getPlanAction());
        if (dto.getDateEntretien() != null) existing.setDateEntretien(dto.getDateEntretien());

        // ✅ Marquer comme validé par SL
        existing.setValideSL(true);
        EntretienMesure updated = repo.save(existing);
        log.info("Entretien mis à jour - valideSL = {}", updated.isValideSL());

        // Mise à jour historique PAQ
        Optional<PaqDossier> paqOpt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);
        if (paqOpt.isPresent()) {
            PaqDossier paq = paqOpt.get();
            String dateFormatted = LocalDate.now().format(DATE_FORMATTER);
            String historique = addHistorique(paq.getHistorique(),
                    new PaqController.HistoriqueEvent(LocalDate.now(),
                            "VALIDATION SL ENTRETIEN DE MESURE",
                            String.format("Entretien de mesure validé par SL le %s", dateFormatted))
            );
            paq.setHistorique(historique);
            paqRepository.save(paq);
        }

        // ✅ ENVOI DES EMAILS - RÉCUPÉRER LES DESTINATAIRES DU DTO
        List<String> destinatairesSelectionnes = dto.getDestinatairesEmails();
        log.info("Destinataires sélectionnés: {}", destinatairesSelectionnes);

        if (destinatairesSelectionnes != null && !destinatairesSelectionnes.isEmpty()) {
            int envois = 0;
            for (String email : destinatairesSelectionnes) {
                if (email != null && !email.isBlank() && email.contains("@")) {
                    try {
                        // ✅ Appeler la méthode d'envoi d'email
                        envoyerEmailConvocation(expediteurEmail, email.trim(), nomCollab, matricule, dto);
                        envois++;
                        log.info("Email envoyé avec succès à {}", email);
                    } catch (Exception e) {
                        log.error("Erreur envoi email à {}: {}", email, e.getMessage(), e);
                    }
                }
            }
            log.info("Total emails envoyés: {} sur {}", envois, destinatairesSelectionnes.size());
        } else {
            log.warn("Aucun destinataire sélectionné - email non envoyé");
        }

        return updated;
    }
    // ─── VALIDATION QM_SEGMENT (1ère validation) ─────────────────────────────────
    public EntretienMesure valider1AvecHistorique(Long id, String matricule,
                                                  EntretienMesureRequestDTO dto,
                                                  String expediteurEmail) {
        EntretienMesure existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Entretien introuvable: " + id));

        if (dto.getTypeFaute() != null) existing.setTypeFaute(dto.getTypeFaute());
        if (dto.getCausesPrincipales() != null) existing.setCausesPrincipales(dto.getCausesPrincipales());
        if (dto.getConvention() != null) existing.setConvention(dto.getConvention());
        if (dto.getPlanAction() != null) existing.setPlanAction(dto.getPlanAction());
        if (dto.getDateEntretien() != null) existing.setDateEntretien(dto.getDateEntretien());
        if (dto.getCasca() != null) existing.setCasca(dto.getCasca()); // NOUVEAU

        existing.setValideQM(true);
        EntretienMesure updated = repo.save(existing);

        Optional<PaqDossier> paqOpt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);
        if (paqOpt.isPresent()) {
            PaqDossier paq = paqOpt.get();
            String dateFormatted = LocalDate.now().format(DATE_FORMATTER);
            String historique = addHistorique(paq.getHistorique(),
                    new PaqController.HistoriqueEvent(LocalDate.now(),
                            "VALIDATION QM ENTRETIEN DE MESURE",
                            String.format("Entretien de mesure validé par QM-Segment le %s ", dateFormatted))
            );
            paq.setHistorique(historique);
            paqRepository.save(paq);
        }

        log.info("Entretien de mesure {} validé par QM-Segment pour {}", id, matricule);
        return updated;
    }

    // ─── VALIDATION SGL (2ème validation) ────────────────────────────────────────
    public EntretienMesure valider2AvecHistorique(Long id, String matricule,
                                                  EntretienMesureRequestDTO dto,
                                                  String expediteurEmail) {
        EntretienMesure existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Entretien introuvable: " + id));

        if (!existing.isValideQM()) {
            throw new RuntimeException("La validation par QM-Segment est requise avant la validation SGL");
        }

        if (dto.getTypeFaute() != null) existing.setTypeFaute(dto.getTypeFaute());
        if (dto.getCausesPrincipales() != null) existing.setCausesPrincipales(dto.getCausesPrincipales());
        if (dto.getConvention() != null) existing.setConvention(dto.getConvention());
        if (dto.getPlanAction() != null) existing.setPlanAction(dto.getPlanAction());
        if (dto.getDateEntretien() != null) existing.setDateEntretien(dto.getDateEntretien());

        existing.setValideSGL(true);
        EntretienMesure updated = repo.save(existing);

        Optional<PaqDossier> paqOpt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);
        if (paqOpt.isPresent()) {
            PaqDossier paq = paqOpt.get();

            if (paq.getNiveau() == 2) {
                paq.setNiveau(3);
                paq.setDateQuatriemeEntretien(LocalDate.now());

                String dateFormatted = LocalDate.now().format(DATE_FORMATTER);
                String historique = addHistorique(paq.getHistorique(),
                        new PaqController.HistoriqueEvent(LocalDate.now(),
                                "VALIDATION SGL ENTRETIEN DE MESURE",
                                String.format("Entretien de mesure validé par SGL le %s ", dateFormatted))
                );
                paq.setHistorique(historique);
                paqRepository.save(paq);

                log.info("PAQ niveau 2 → 3 après validation SGL pour {}", matricule);
            }
        }

        log.info("Entretien de mesure {} validé par SGL pour {}", id, matricule);
        return updated;
    }

    // ─── SUPPRESSION ─────────────────────────────────────────────────────────────
    public void deleteAvecNotification(Long id, String matricule, String expediteurEmail,
                                       String destinataireEmail, String nomCollab) {
        repo.findById(id).orElseThrow(() -> new RuntimeException("Entretien introuvable: " + id));
        repo.deleteById(id);

        Optional<PaqDossier> paqOpt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);
        if (paqOpt.isPresent()) {
            PaqDossier paq = paqOpt.get();
            String dateFormatted = LocalDate.now().format(DATE_FORMATTER);
            String historique = addHistorique(paq.getHistorique(),
                    new PaqController.HistoriqueEvent(LocalDate.now(),
                            "SUPPRESSION ENTRETIEN DE MESURE",
                            String.format("Entretien de mesure supprimé le %s", dateFormatted))
            );
            paq.setHistorique(historique);
            paqRepository.save(paq);
        }

        log.info("Entretien de mesure {} supprimé pour {}", id, matricule);
    }

    // ─── UTILITAIRES ─────────────────────────────────────────────────────────────
    public List<EntretienMesure> getByMatricule(String matricule) {
        return repo.findByMatricule(matricule);
    }

    public EntretienMesure findById(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Entretien introuvable id=" + id));
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public String getCollaborateurNom(String matricule) {
        try {
            return collaboratorRepository.findByMatricule(matricule)
                    .map(c -> c.getName() + " " + c.getPrenom())
                    .orElse(matricule);
        } catch (Exception e) {
            return matricule;
        }
    }



    // ─── EMAILS ─────────────────────────────────────────────────────────────────
    private void envoyerEmailConvocation(String expediteur, String destinataire,
                                         String nomCollab, String matricule,
                                         EntretienMesureRequestDTO dto) {
        try {
            String sujet = String.format("[PAQ]  Entretien de mesure - %s", nomCollab);

            // Formater la date correctement
            String dateEntretienStr = dto.getDateEntretien() != null
                    ? dto.getDateEntretien().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "Non définie";

            String htmlContent = String.format("""
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"></head>
        <body style="font-family:Arial,sans-serif;background:#f4f4f4;padding:20px;">
          <div style="max-width:600px;margin:auto;background:white;border-radius:10px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)">
            <div style="background:#C8102E;padding:20px;text-align:center;">
              <h2 style="color:white;margin:0;"> Entretien de Mesure</h2>
              <p style="color:#ffcccc;margin:5px 0 0;">PAQ - Leoni</p>
            </div>
            <div style="padding:20px;">
              <p>Bonjour,</p>
              <p><strong>Merci d'assister à l'entretien de mesure</strong></p>
              <table style="width:100%%;border-collapse:collapse;margin:20px 0;">
                <tr style="background:#f8f9fa;">
                  <td style="padding:10px;border:1px solid #ddd;font-weight:600;width:40%%">Collaborateur</td>
                  <td style="padding:10px;border:1px solid #ddd;"><strong>%s</strong></td>
                </tr>
                <tr>
                  <td style="padding:10px;border:1px solid #ddd;font-weight:600;">Matricule</td>
                  <td style="padding:10px;border:1px solid #ddd;"><code>%s</code></td>
                </tr>
                <tr style="background:#f8f9fa;">
                  <td style="padding:10px;border:1px solid #ddd;font-weight:600;">Type de faute</td>
                  <td style="padding:10px;border:1px solid #ddd;">%s</td>
                </tr>
                <tr>
                  <td style="padding:10px;border:1px solid #ddd;font-weight:600;">Date entretien</td>
                  <td style="padding:10px;border:1px solid #ddd;">%s</td>
                </tr>
              </table>
            </div>
            <div style="background:#f8f9fa;padding:10px;text-align:center;font-size:11px;color:#888;">
              Email automatique - Système PAQ LEONI
            </div>
          </div>
        </body>
        </html>
        """, nomCollab, matricule,
                    dto.getTypeFaute() != null ? dto.getTypeFaute() : "Non spécifié",
                    dateEntretienStr);

            emailService.sendEmail(expediteur, destinataire, sujet, htmlContent);
            log.info("✅ Email convocation mesure envoyé à {} pour {}", destinataire, matricule);
        } catch (Exception e) {
            log.error("❌ Erreur envoi email à {}: {}", destinataire, e.getMessage(), e);
        }
    }
    private String buildEmailConvocationContent(String nomCollab, String matricule, EntretienMesureRequestDTO dto) {
        return String.format("""
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"></head>
        <body style="font-family:Arial,sans-serif;">
          <div style="max-width:600px;margin:auto;background:white;border-radius:8px;padding:20px;">
            <div style="background:#C8102E;padding:15px;border-radius:8px 8px 0 0;margin:-20px -20px 0 -20px;">
              <h2 style="color:white;margin:0;">PAQ - Entretien de Mesure</h2>
            </div>
            <div style="padding:20px 0;">
              <p>Bonjour,</p>
              <p><strong>Merci d'assister à l'entretien de mesure</strong></p>
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
                <tr><td style="padding:8px;border:1px solid #ddd;"><strong>Date requalification</strong></td>
                    <td style="padding:8px;border:1px solid #ddd;">%s</td>
                </tr>
               </table>
              <p>Veuillez vous connecter au système PAQ pour valider cet entretien.</p>
            </div>
          </div>
        </body>
        </html>
        """, nomCollab, matricule,
                dto.getTypeFaute() != null ? dto.getTypeFaute() : "",
                dto.getDateEntretien() != null ? dto.getDateEntretien().toString() : "");



    }
}