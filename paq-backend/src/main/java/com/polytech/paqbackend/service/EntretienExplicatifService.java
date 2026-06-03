package com.polytech.paqbackend.service;

import com.polytech.paqbackend.controller.PaqController;
import com.polytech.paqbackend.dto.EntretienExplicatifDTO;
import com.polytech.paqbackend.entity.Collaborator;
import com.polytech.paqbackend.entity.EntretienExplicatif;
import com.polytech.paqbackend.entity.PaqDossier;
import com.polytech.paqbackend.entity.User;
import com.polytech.paqbackend.repository.CollaboratorRepository;
import com.polytech.paqbackend.repository.EntretienExplicatifRepository;
import com.polytech.paqbackend.repository.PaqRepository;
import com.polytech.paqbackend.repository.UserRepository;
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
public class EntretienExplicatifService {

    private static final Logger log = LoggerFactory.getLogger(EntretienExplicatifService.class);

    private final EntretienExplicatifRepository entretienRepo;
    private final CollaboratorRepository collaborateurRepo;
    private final PaqRepository paqRepository;
    private final ObjectMapper objectMapper;

    private final UserRepository userRepository;
    private final EmailService emailService;

    public EntretienExplicatifService(EntretienExplicatifRepository entretienRepo,
                                      CollaboratorRepository collaborateurRepo,
                                      PaqRepository paqRepository,
                                      UserRepository userRepository,                    // ← AJOUT
                                       EmailService emailService) {
        this.entretienRepo = entretienRepo;
        this.collaborateurRepo = collaborateurRepo;
        this.paqRepository = paqRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.userRepository  = userRepository;
        this.emailService    = emailService;

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

    // Méthode create avec 3 paramètres (matricule, dto, expediteurEmail)
    // ── Méthode create ────────────────────────────────────────────────────────
    public EntretienExplicatif create(String matricule,
                                      EntretienExplicatifDTO dto,
                                      String expediteurEmail) {
        EntretienExplicatif e = new EntretienExplicatif();
        e.setMatricule(matricule);
        mapDtoToEntity(dto, e);
        EntretienExplicatif saved = entretienRepo.save(e);

        Optional<PaqDossier> paqOpt = paqRepository
                .findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);

        if (paqOpt.isPresent()) {
            PaqDossier paq = paqOpt.get();

            if (paq.getNiveau() > 1) {
                throw new RuntimeException("Le niveau actuel (" + paq.getNiveau()
                        + ") ne permet pas l'entretien explicatif");
            }

            LocalDate dateEntretien = dto.getDate() != null ? dto.getDate() : LocalDate.now();

            if (paq.getNiveau() == 0) {
                paq.setNiveau(1);
                paq.setDatePremierEntretien(dateEntretien);
            }



            String notes = "Type faute: " + dto.getTypeFaute()
                    + " | Description: " + (dto.getDescription() != null ? dto.getDescription() : "")
                    + " | Mesures: "     + (dto.getMesuresCorrectives() != null ? dto.getMesuresCorrectives() : "")
                    + (dto.isDefautGrave() ? " | DÉFAUT GRAVE" : "");
            paq.setPremierEntretienNotes(notes);

            String actionLabel = dto.isDefautGrave()
                    ? "ENTRETIEN EXPLICATIF — DÉFAUT GRAVE"
                    : "ENTRETIEN EXPLICATIF";

            String historique = addHistorique(
                    paq.getHistorique(),
                    new PaqController.HistoriqueEvent(
                            dateEntretien,
                            actionLabel,
                            String.format("Entretien explicatif créé le %s — Faute : %s%s",
                                    dateEntretien,
                                    dto.getTypeFaute(),
                                    dto.isDefautGrave() ? " (DÉFAUT GRAVE — SGL notifié)" : "")
                    )
            );
            paq.setHistorique(historique);
            paqRepository.save(paq);

            // ── Envoi mail SGL si défaut grave ────────────────────────────────
            if (dto.isDefautGrave()) {
                notifierSGL(matricule, dto.getTypeFaute(), expediteurEmail);
            }

            log.info("PAQ mis à jour pour le matricule {}", matricule);
        } else {
            log.warn("Aucun PAQ actif trouvé pour le matricule {}", matricule);
        }

        return saved;
    }

    // ── Méthode update ────────────────────────────────────────────────────────
    public EntretienExplicatif update(Long id,
                                      String matricule,
                                      EntretienExplicatifDTO dto,
                                      String expediteurEmail) {
        EntretienExplicatif existing = entretienRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Entretien introuvable: " + id));

        boolean etaitDejaDefautGrave = existing.isDefautGrave();
        mapDtoToEntity(dto, existing);
        EntretienExplicatif updated = entretienRepo.save(existing);

        Optional<PaqDossier> paqOpt = paqRepository
                .findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);

        if (paqOpt.isPresent()) {
            PaqDossier paq = paqOpt.get();

            String notes = "Type faute: " + dto.getTypeFaute()
                    + " | Description: " + (dto.getDescription() != null ? dto.getDescription() : "")
                    + " | Mesures: "     + (dto.getMesuresCorrectives() != null ? dto.getMesuresCorrectives() : "")
                    + (dto.isDefautGrave() ? " | DÉFAUT GRAVE" : "");
            paq.setPremierEntretienNotes(notes);

            String historique = addHistorique(
                    paq.getHistorique(),
                    new PaqController.HistoriqueEvent(
                            LocalDate.now(),
                            "MODIFICATION ENTRETIEN EXPLICATIF",
                            String.format("Entretien modifié le %s par %s%s",
                                    LocalDate.now(),
                                    expediteurEmail,
                                    dto.isDefautGrave() ? " (DÉFAUT GRAVE)" : "")
                    )
            );
            paq.setHistorique(historique);
            paqRepository.save(paq);

            // Envoyer le mail SGL seulement si défaut grave NOUVELLEMENT coché
            if (dto.isDefautGrave() && !etaitDejaDefautGrave) {
                notifierSGL(matricule, dto.getTypeFaute(), expediteurEmail);
            }
        }

        return updated;
    }

    // ── Méthode privée : chercher les SGL du segment et envoyer le mail ───────
    // EntretienExplicatifService.java — méthode notifierSGL()
// REMPLACER :
    private void notifierSGL(String matricule, String typeFaute, String expediteurEmail) {
        try {
            Optional<Collaborator> collabOpt = collaborateurRepo.findById(matricule);
            if (collabOpt.isEmpty()) {
                log.warn("Collaborateur {} introuvable pour notification SGL", matricule);
                return;
            }
            Collaborator collab = collabOpt.get();
            String collaborateurNom = collab.getName() + " " + collab.getPrenom();

            // ✅ segment est un String — on cherche les SGL par nom de segment
            String segmentNom = collab.getSegment(); // String directement
            if (segmentNom == null || segmentNom.isBlank()) {
                log.warn("Segment null pour le collaborateur {}", matricule);
                return;
            }

            // ✅ Utiliser la méthode adaptée à votre User/Segment
            List<User> sgls = userRepository.findSGLBySegmentNom(segmentNom);

            if (sgls.isEmpty()) {
                log.warn("Aucun SGL trouvé pour le segment '{}' (matricule {})",
                        segmentNom, matricule);
                return;
            }

            for (User sgl : sgls) {
                String html = buildDefautGraveEmail(
                        sgl.getNomUtilisateur() != null
                                ? sgl.getNomUtilisateur() : sgl.getLogin(),
                        collaborateurNom,
                        matricule,
                        segmentNom,
                        typeFaute
                );
                emailService.sendEmail(
                        expediteurEmail,
                        sgl.getEmail(),
                        "[PAQ] ⚠ Défaut grave — Participation SGL obligatoire",
                        html
                );
                log.info("Notification défaut grave envoyée au SGL {} pour matricule {}",
                        sgl.getEmail(), matricule);
            }
        } catch (Exception e) {
            log.error("Erreur notification SGL pour {} : {}", matricule, e.getMessage());
        }
    }

    // Template HTML pour le mail défaut grave
    private String buildDefautGraveEmail(String sglNom, String collaborateurNom,
                                         String matricule, String segment,
                                         String typeFaute) {
        return String.format("""
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"></head>
        <body style="font-family:Arial,sans-serif;background:#f4f4f4;padding:20px;">
          <div style="max-width:620px;margin:auto;background:#fff;
                      border-radius:10px;overflow:hidden;
                      box-shadow:0 2px 8px rgba(0,0,0,.1)">
            <div style="background:#b91c1c;padding:24px 30px;">
              <h2 style="color:#fff;margin:0;font-size:18px;">
                ⚠ Défaut grave détecté — Action requise
              </h2>
            </div>
            <div style="padding:28px 30px;">
              <p>Bonjour <strong>%s</strong>,</p>
                            <p><strong>Merci d'assister à l'entretien de décision</strong></p>
              <p>Un <strong>défaut grave</strong> a été enregistré.
                 Votre participation à l'entretien explicatif est
                 <strong>obligatoire dès le niveau 1</strong>.</p>
              <table style="width:100%%;border-collapse:collapse;margin:20px 0;">
                <tr style="background:#fef2f2;">
                  <td style="padding:10px;border:1px solid #fecaca;
                             font-weight:600;width:40%%">Collaborateur</td>
                  <td style="padding:10px;border:1px solid #fecaca;">
                    <strong>%s</strong></td>
                </tr>
                <tr>
                  <td style="padding:10px;border:1px solid #fecaca;
                             font-weight:600;">Matricule</td>
                  <td style="padding:10px;border:1px solid #fecaca;">
                    <code>%s</code></td>
                </tr>
                <tr style="background:#fef2f2;">
                  <td style="padding:10px;border:1px solid #fecaca;
                             font-weight:600;">Segment</td>
                  <td style="padding:10px;border:1px solid #fecaca;">%s</td>
                </tr>
                <tr>
                  <td style="padding:10px;border:1px solid #fecaca;
                             font-weight:600;">Type de faute</td>
                  <td style="padding:10px;border:1px solid #fecaca;">
                    <strong style="color:#b91c1c;">%s</strong></td>
                </tr>
              </table>
              <p style="background:#fef3c7;border-left:4px solid #f59e0b;
                         padding:12px 16px;border-radius:0 6px 6px 0;">
                Connectez-vous à la plateforme PAQ pour consulter
                et valider cet entretien.
              </p>
            </div>
            <div style="background:#f8f9fa;padding:14px 30px;
                        font-size:11px;color:#888;text-align:center;">
              Email automatique — Système PAQ LEONI
            </div>
          </div>
        </body>
        </html>
        """,
                sglNom, collaborateurNom, matricule, segment, typeFaute
        );
    }


    // Méthode validate avec 2 paramètres (id, expediteurEmail)
    public void validate(Long id, String expediteurEmail) {
        EntretienExplicatif entretien = entretienRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Entretien introuvable: " + id));

        Optional<PaqDossier> paqOpt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(entretien.getMatricule());

        if (paqOpt.isPresent()) {
            PaqDossier paq = paqOpt.get();

            // Vérifier si l'entretien a déjà été validé pour éviter les doublons
            boolean alreadyValidated = false;
            if (paq.getHistorique() != null && !paq.getHistorique().isEmpty()) {
                try {
                    List<PaqController.HistoriqueEvent> historique = objectMapper.readValue(
                            paq.getHistorique(),
                            new TypeReference<List<PaqController.HistoriqueEvent>>() {});
                    alreadyValidated = historique.stream()
                            .anyMatch(event -> "VALIDATION ENTRETIEN EXPLICATIF".equals(event.getAction()));
                } catch (Exception e) {
                    log.warn("Erreur lors de la vérification de l'historique", e);
                }
            }

            // Ajouter la validation seulement si elle n'existe pas déjà
            if (!alreadyValidated) {
                String historique = addHistorique(
                        paq.getHistorique(),
                        new PaqController.HistoriqueEvent(
                                LocalDate.now(),
                                "VALIDATION ENTRETIEN EXPLICATIF",
                                String.format("Entretien explicatif validé le %s par %s", LocalDate.now(), expediteurEmail)
                        )
                );
                paq.setHistorique(historique);
                paqRepository.save(paq);
                log.info("Entretien explicatif {} validé par {}", id, expediteurEmail);
            } else {
                log.info("Entretien explicatif {} déjà validé, aucune nouvelle entrée d'historique ajoutée", id);
            }
        }
    }

    public List<EntretienExplicatif> findByMatricule(String matricule) {
        return entretienRepo.findByMatricule(matricule);
    }

    public EntretienExplicatif findById(Long id) {
        return entretienRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Entretien introuvable: " + id));
    }


    private void mapDtoToEntity(EntretienExplicatifDTO dto, EntretienExplicatif e) {
        e.setTypeFaute(dto.getTypeFaute());
        e.setDateFaute(dto.getDate());
        e.setDescription(dto.getDescription());
        e.setMesuresCorrectives(dto.getMesuresCorrectives());
        e.setCommentaire(dto.getNotes());
        e.setDefautGrave(dto.isDefautGrave());
    }
}