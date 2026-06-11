package com.polytech.paqbackend.service;

import com.polytech.paqbackend.entity.Archive;
import com.polytech.paqbackend.entity.Collaborator;
import com.polytech.paqbackend.entity.PaqDossier;
import com.polytech.paqbackend.repository.ArchiveRepository;
import com.polytech.paqbackend.repository.CollaboratorRepository;
import com.polytech.paqbackend.repository.PaqRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ArchivingService {
    private LocalDate simulatedToday = null;
    private final PaqRepository            paqRepository;
    private final ArchiveRepository        archiveRepository;
    private final CollaboratorRepository   collaboratorRepository;

    public ArchivingService(PaqRepository paqRepository,
                            ArchiveRepository archiveRepository,
                            CollaboratorRepository collaboratorRepository) {
        this.paqRepository          = paqRepository;
        this.archiveRepository      = archiveRepository;
        this.collaboratorRepository = collaboratorRepository;
    }

    /**
     * Archive automatiquement les PAQ actifs dont dateFin <= aujourd'hui (6 mois écoulés)
     * et qui n'ont pas encore été archivés.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void archiveExpiredPaqs() {
        LocalDate today = (simulatedToday != null) ? simulatedToday : LocalDate.now();

        // Récupérer les PAQ actifs non archivés dont la date de fin est dépassée
        List<PaqDossier> activePaqs = paqRepository.findByActifTrueAndArchivedFalse();

        int count = 0;
        for (PaqDossier paq : activePaqs) {

            // Vérifier que la période de 6 mois est bien écoulée
            if (paq.getDateFin() == null || paq.getDateFin().isAfter(today)) {
                continue;
            }

            // Éviter les doublons
            if (archiveRepository.existsByPaqDossierId(paq.getId())) {
                // Marquer quand même comme archivé si ce n'est pas fait
                if (!paq.isArchived()) {
                    paq.setArchived(true);
                    paq.setActif(false);
                    paq.setStatut("ARCHIVE");
                    paq.setDateArchivage(today);
                    paqRepository.save(paq);
                }
                continue;
            }

            // Récupérer le nom complet du collaborateur
            String nomPrenom = paq.getCollaboratorMatricule();
            Optional<Collaborator> collabOpt =
                    collaboratorRepository.findByMatricule(paq.getCollaboratorMatricule());
            if (collabOpt.isPresent()) {
                Collaborator c = collabOpt.get();
                nomPrenom = c.getName() + " " + c.getPrenom();
            }

            // Marquer le PAQ comme archivé
            paq.setStatut("ARCHIVE");
            paq.setArchived(true);
            paq.setActif(false);
            paq.setDateArchivage(today);
            paqRepository.save(paq);

            // Créer l'entrée Archive enrichie (avec historique, niveau, statut, dates)
            Archive archive = new Archive();
            archive.setType(resolveTypeEntretien(paq));
            archive.setMatricule(paq.getCollaboratorMatricule());
            archive.setNomPrenom(nomPrenom);
            archive.setDateArchivage(today);
            archive.setPaqDossierId(paq.getId());
            archive.setNiveau(paq.getNiveau());
            archive.setStatut(paq.getStatut());
            archive.setHistorique(paq.getHistorique());
            archive.setDateCreation(paq.getDateCreation());
            archive.setDateFin(paq.getDateFin());
            archiveRepository.save(archive);

            count++;
            System.out.println("[ArchivingService] PAQ archivé automatiquement pour: "
                    + paq.getCollaboratorMatricule() + " (période: "
                    + paq.getDateCreation() + " → " + paq.getDateFin() + ")");
        }

        System.out.println("[ArchivingService] Archivage automatique terminé : " + count + " dossier(s) PAQ");
    }

    /**
     * Détermine le type d'entretien le plus avancé réalisé dans le PAQ.
     */
    private String resolveTypeEntretien(PaqDossier paq) {
        if (paq.getDateCinquiemeEntretien() != null) return "Entretien Final";
        if (paq.getDateQuatriemeEntretien() != null) return "Entretien De decision";
        if (paq.getDateTroisiemeEntretien() != null) return "Entretien De mesure";
        if (paq.getDateDeuxiemeEntretien() != null) return "Entretien D'accord";
        if (paq.getDatePremierEntretien() != null) return "Entretien Explicatif";
        return "PAQ";
    }

    public void setSimulatedToday(LocalDate simulatedToday) {
        this.simulatedToday = simulatedToday;
    }
}