package com.polytech.paqbackend.service;




import com.polytech.paqbackend.entity.Collaborator;
import com.polytech.paqbackend.entity.PaqDossier;

import com.polytech.paqbackend.repository.CollaboratorRepository;
import com.polytech.paqbackend.repository.PaqRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.temporal.ChronoUnit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PaqSchedulerService {

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private PaqRepository paqRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Add missing dependency
    @Autowired
    private EntretienPositifService entretienPositifService;


    @Autowired
    private EntretienMesureService service;



    // Classe interne pour l'historique
    public static class HistoriqueEvent {
        private LocalDate date;
        private String action;
        private String detail;

        public HistoriqueEvent() {}

        public HistoriqueEvent(LocalDate date, String action, String detail) {
            this.date = date;
            this.action = action;
            this.detail = detail;
        }

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getDetail() { return detail; }
        public void setDetail(String detail) { this.detail = detail; }
    }

    private String addHistorique(String historiqueJson, HistoriqueEvent event) {
        try {
            List<HistoriqueEvent> list;
            if (historiqueJson == null || historiqueJson.isBlank() || "[]".equals(historiqueJson)) {
                list = new ArrayList<>();
            } else {
                list = objectMapper.readValue(historiqueJson, new TypeReference<List<HistoriqueEvent>>() {});
            }
            list.add(event);
            list.sort(Comparator.comparing(HistoriqueEvent::getDate));
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            e.printStackTrace();
            return String.format("[{\"date\":\"%s\",\"action\":\"%s\",\"detail\":\"%s\"}]",
                    event.getDate(), event.getAction(), event.getDetail());
        }
    }

    /**
     * Crée automatiquement un nouveau PAQ tous les 6 mois pour chaque collaborateur actif
     * Vérifie tous les jours à minuit
     */
    @Transactional
    public void createPeriodicPaq() {
        System.out.println("=== Création automatique des PAQ périodiques ===");
        LocalDate now = LocalDate.now();
        List<Collaborator> activeCollaborators = collaboratorRepository.findByDepartFalseAndArchivedFalse();

        int createdCount = 0;

        for (Collaborator collab : activeCollaborators) {
            if (collab.getHireDate() == null) continue;

            // Vérifier que le collaborateur a au moins 6 mois d'ancienneté
            if (ChronoUnit.MONTHS.between(collab.getHireDate(), now) < 6) {
                continue;
            }

            // Récupérer le dernier PAQ de ce collaborateur
            List<PaqDossier> paqHistory = paqRepository.findAllByCollaboratorMatriculeOrderByDateCreationDesc(collab.getMatricule());
            PaqDossier lastPaq = paqHistory.isEmpty() ? null : paqHistory.get(0);

            // Si pas de PAQ
            if (lastPaq == null) {
                // Créer le premier PAQ
                PaqDossier newPaq = new PaqDossier();
                newPaq.setCollaboratorMatricule(collab.getMatricule());
                newPaq.setDateCreation(collab.getHireDate()); // Utiliser la date d'embauche comme date de création
                newPaq.setDateFin(collab.getHireDate().plusMonths(6));
                newPaq.setCreatedAt(LocalDateTime.now());
                newPaq.setNiveau(0);
                newPaq.setStatut("EN_COURS");
                newPaq.setActif(true);
                newPaq.setArchived(false);
                newPaq.setDerniereFaute(null);

                String historique = addHistorique(null, new HistoriqueEvent(
                        collab.getHireDate(),
                        "Création du dossier PAQ",
                        String.format("Dossier créé après %d mois d'ancienneté",
                                ChronoUnit.MONTHS.between(collab.getHireDate(), now))
                ));
                newPaq.setHistorique(historique);

                paqRepository.save(newPaq);
                createdCount++;
                System.out.println("Premier PAQ créé pour: " + collab.getMatricule());
            }
            // Si le dernier PAQ date de plus de 6 mois
            else if (lastPaq.getDateCreation().plusMonths(6).isBefore(now)) {
                // Archiver l'ancien PAQ
                lastPaq.setArchived(true);
                lastPaq.setActif(false);
                lastPaq.setStatut("ARCHIVE");
                String archiveHistorique = addHistorique(lastPaq.getHistorique(),
                        new HistoriqueEvent(now, "Archivage automatique",
                                "Fin de période - Nouveau PAQ créé"));
                lastPaq.setHistorique(archiveHistorique);
                paqRepository.save(lastPaq);

                // Créer le nouveau PAQ
                PaqDossier newPaq = new PaqDossier();
                newPaq.setCollaboratorMatricule(collab.getMatricule());
                newPaq.setDateCreation(now);
                newPaq.setDateFin(now.plusMonths(6));
                newPaq.setCreatedAt(LocalDateTime.now());
                newPaq.setNiveau(0);
                newPaq.setStatut("EN_COURS");
                newPaq.setActif(true);
                newPaq.setArchived(false);
                newPaq.setDerniereFaute(null);

                int periode = paqHistory.size() + 1;
                String historique = addHistorique(null, new HistoriqueEvent(
                        now,
                        "DOSSIER PAQ CRÉÉ",
                        String.format("Période %d: %s - %s", periode, now, now.plusMonths(6))
                ));
                newPaq.setHistorique(historique);

                paqRepository.save(newPaq);
                createdCount++;
                System.out.println("Nouveau PAQ créé pour: " + collab.getMatricule() + " (Période " + periode + ")");
            }
        }

        System.out.println("Création PAQ périodique terminée: " + createdCount + " dossiers créés");
    }

    /**
     * Archive automatiquement les PAQ arrivés à terme (6 mois)
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void archiveExpiredPaq() {
        System.out.println("=== Archivage des PAQ expirés ===");
        LocalDate now = LocalDate.now();

        List<PaqDossier> activePaqs = paqRepository.findByActifTrueAndArchivedFalse();
        int archivedCount = 0;

        for (PaqDossier paq : activePaqs) {
            if (paq.getDateFin() != null && paq.getDateFin().isBefore(now)) {
                paq.setArchived(true);
                paq.setActif(false);
                paq.setStatut("ARCHIVE");

                String newHistorique = addHistorique(paq.getHistorique(),
                        new HistoriqueEvent(now, "Archivage automatique",
                                "Fin de période de 6 mois - Dossier archivé"));
                paq.setHistorique(newHistorique);

                paqRepository.save(paq);
                archivedCount++;
                System.out.println("PAQ archivé pour: " + paq.getCollaboratorMatricule());
            }
        }

        System.out.println("Archivage terminé: " + archivedCount + " dossiers archivés");
    }

    /**
     * Gère les départs des collaborateurs
     * Archive les PAQ en lecture seule pendant 6 mois après le départ
     */
    @Scheduled(cron = "0 0 0  * * ?")
    @Transactional
    public void handleDepartures() {
        System.out.println("=== Gestion des départs collaborateurs ===");
        LocalDate now = LocalDate.now();

        List<Collaborator> departedCollaborators = collaboratorRepository.findByDepartTrueAndArchivedFalse();

        for (Collaborator collab : departedCollaborators) {
            LocalDate departureDate = collab.getDepartDate(); // Changed from getDepart() to getDepartDate()
            if (departureDate == null) continue;

            // Si le départ date de plus de 6 mois, archiver définitivement
            if (departureDate.plusMonths(6).isBefore(now)) {
                collab.setArchived(true);
                collaboratorRepository.save(collab);

                // Archiver tous les PAQ de ce collaborateur
                List<PaqDossier> paqs = paqRepository.findAllByCollaboratorMatriculeOrderByDateCreationDesc(collab.getMatricule());
                for (PaqDossier paq : paqs) {
                    if (!paq.isArchived()) {
                        paq.setArchived(true);
                        paq.setActif(false);
                        paq.setStatut("ARCHIVE_DEPART");
                        String newHistorique = addHistorique(paq.getHistorique(),
                                new HistoriqueEvent(now, "Archivage définitif",
                                        "Collaborateur parti depuis plus de 6 mois - Archivage définitif"));
                        paq.setHistorique(newHistorique);
                        paqRepository.save(paq);
                    }
                }
                System.out.println("Collaborateur archivé définitivement: " + collab.getMatricule());
            }
            // Sinon, s'assurer que les PAQ sont en lecture seule
            else {
                List<PaqDossier> activePaqs = paqRepository.findByActifTrueAndArchivedFalse();
                for (PaqDossier paq : activePaqs) {
                    if (paq.getCollaboratorMatricule().equals(collab.getMatricule())) {
                        paq.setStatut("DEPART_READONLY");
                        String newHistorique = addHistorique(paq.getHistorique(),
                                new HistoriqueEvent(now, "Départ collaborateur",
                                        "Dossier en lecture seule pour 6 mois suite au départ"));
                        paq.setHistorique(newHistorique);
                        paqRepository.save(paq);
                        System.out.println("PAQ mis en lecture seule pour: " + collab.getMatricule());
                    }
                }
            }
        }
    }






}


