package com.polytech.paqbackend.controller;

import com.polytech.paqbackend.entity.Archive;
import com.polytech.paqbackend.entity.Collaborator;
import com.polytech.paqbackend.entity.PaqDossier;
import com.polytech.paqbackend.repository.ArchiveRepository;
import com.polytech.paqbackend.repository.CollaboratorRepository;
import com.polytech.paqbackend.repository.PaqRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/paq")
public class PaqController {

    @Autowired
    private PaqRepository paqRepository;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private ArchiveRepository archiveRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();



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

    public static class EntretienRequest {
        private String notes;
        private LocalDate date;
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
    }

    public static class FauteRequest {
        private String detail;
        private String type;
        public String getDetail() { return detail; }
        public void setDetail(String detail) { this.detail = detail; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
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
            return String.format("[{\"date\":\"%s\",\"action\":\"%s\",\"detail\":\"%s\"}]",
                    event.getDate(), event.getAction(), event.getDetail());
        }
    }



    /**
     * Détermine le type d'entretien le plus avancé du PAQ pour l'archive.
     */
    private String resolveTypeEntretien(PaqDossier paq) {
        if (paq.getDateCinquiemeEntretien() != null) return "Entretien Final";
        if (paq.getDateQuatriemeEntretien() != null) return "Entretien De decision";
        if (paq.getDateTroisiemeEntretien() != null) return "Entretien De mesure";
        if (paq.getDateDeuxiemeEntretien() != null) return "Entretien D'accord";
        if (paq.getDatePremierEntretien() != null) return "Entretien Explicatif";
        return "PAQ";
    }

    /**
     * Crée une entrée Archive à partir d'un PAQ archivé.
     * Stocke aussi le niveau, le statut et l'historique complet pour l'export PDF.
     */
    private void createArchiveEntry(PaqDossier paq, String nomPrenom) {
        // Éviter les doublons
        if (archiveRepository.existsByPaqDossierId(paq.getId())) return;

        Archive archive = new Archive();
        archive.setType(resolveTypeEntretien(paq));
        archive.setMatricule(paq.getCollaboratorMatricule());
        archive.setNomPrenom(nomPrenom);
        archive.setDateArchivage(LocalDate.now());
        archive.setPaqDossierId(paq.getId());

        // Champs supplémentaires pour export PDF — nécessite les setters dans Archive.java
        archive.setNiveau(paq.getNiveau());
        archive.setStatut(paq.getStatut());
        archive.setHistorique(paq.getHistorique());
        archive.setDateCreation(paq.getDateCreation());
        archive.setDateFin(paq.getDateFin());

        archiveRepository.save(archive);
    }



    @GetMapping("/{matricule}")
    public ResponseEntity<?> getPaqByMatricule(@PathVariable String matricule) {
        Optional<PaqDossier> paqOpt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);
        if (paqOpt.isPresent()) return ResponseEntity.ok(paqOpt.get());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Aucun dossier PAQ actif trouvé pour le matricule: " + matricule));
    }

    @GetMapping("/history/{matricule}")
    public ResponseEntity<?> getAllPaqsByMatricule(@PathVariable String matricule) {
        return ResponseEntity.ok(paqRepository.findAllByCollaboratorMatriculeOrderByDateCreationDesc(matricule));
    }

    @GetMapping("/all")
    public ResponseEntity<List<PaqDossier>> getAllPaqs() {
        return ResponseEntity.ok(paqRepository.findAll());
    }

    @GetMapping("/{matricule}/history")
    public ResponseEntity<?> getHistory(@PathVariable String matricule) {
        Optional<PaqDossier> paqOpt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);
        if (paqOpt.isEmpty()) return ResponseEntity.ok(new ArrayList<>());
        try {
            List<HistoriqueEvent> events = objectMapper.readValue(
                    paqOpt.get().getHistorique(),
                    new TypeReference<List<HistoriqueEvent>>() {}
            );
            events.sort(Comparator.comparing(HistoriqueEvent::getDate));
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }



    // Remplacer la méthode peutCreerPaq
    private boolean peutCreerPaq(Collaborator collaborator) {
        if (collaborator.getHireDate() == null) return false;

        // Chercher le dernier PAQ (même archivé)
        List<PaqDossier> allPaqs = paqRepository.findAllByCollaboratorMatriculeOrderByDateCreationDesc(collaborator.getMatricule());

        if (allPaqs.isEmpty()) {
            // Premier PAQ : pas de condition d'ancienneté
            return true;
        }

        // PAQ suivant : attendre 6 mois depuis la création du dernier PAQ
        LocalDate dernierPaqDate = allPaqs.get(0).getDateCreation();
        LocalDate sixMoisApres = dernierPaqDate.plusMonths(6);
        return !LocalDate.now().isBefore(sixMoisApres);
    }

    // Modifier la méthode createPaq
    @PostMapping("/create/{matricule}")
    public ResponseEntity<?> createPaq(@PathVariable String matricule) {
        Optional<Collaborator> collabOpt = collaboratorRepository.findById(matricule);
        if (collabOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Collaborateur non trouvé"));
        }

        Collaborator collaborator = collabOpt.get();

        // Vérifier si on peut créer un PAQ
        if (!peutCreerPaq(collaborator)) {
            List<PaqDossier> allPaqs = paqRepository.findAllByCollaboratorMatriculeOrderByDateCreationDesc(matricule);
            if (!allPaqs.isEmpty()) {
                LocalDate dernierPaqDate = allPaqs.get(0).getDateCreation();
                LocalDate prochainPaqDate = dernierPaqDate.plusMonths(6);
                return ResponseEntity.badRequest().body(Map.of("message",
                        String.format("Un nouveau PAQ peut être créé à partir du %s", prochainPaqDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
            }
            return ResponseEntity.badRequest().body(Map.of("message", "Impossible de créer un PAQ"));
        }

        // Vérifier s'il existe déjà un PAQ actif
        Optional<PaqDossier> existing = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);
        if (existing.isPresent()) {
            return ResponseEntity.ok(existing.get());
        }

        PaqDossier paq = new PaqDossier();
        paq.setCollaboratorMatricule(matricule);
        paq.setDateCreation(LocalDate.now());
        paq.setDateFin(LocalDate.now().plusMonths(6));
        paq.setCreatedAt(LocalDateTime.now());
        paq.setNiveau(0);
        paq.setStatut("EN_COURS");
        paq.setActif(true);
        paq.setArchived(false);
        paq.setDerniereFaute(null);

        // Calculer l'ancienneté pour l'historique
        long moisAnciennete = ChronoUnit.MONTHS.between(collaborator.getHireDate(), LocalDate.now());
        String historique = addHistorique(null, new HistoriqueEvent(
                LocalDate.now(),
                "Création du dossier PAQ",
                String.format("Dossier créé - Ancienneté: %d mois", moisAnciennete)
        ));
        paq.setHistorique(historique);

        PaqDossier saved = paqRepository.save(paq);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }



    /**
     * Enregistre une faute.
     * Règle métier : une faute n'est enregistrable QUE si niveau === 0 et aucune faute déjà déclarée.
     * Après la faute, le frontend affiche automatiquement le bouton "Entretien Explicatif".
     */
    @PostMapping("/{matricule}/faute")
    public ResponseEntity<?> enregistrerFaute(
            @PathVariable String matricule,
            @RequestBody FauteRequest request) {

        Optional<PaqDossier> paqOpt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);
        if (paqOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Aucun dossier PAQ actif trouvé"));

        PaqDossier paq = paqOpt.get();

        if (paq.isArchived() || "CLOTURE".equals(paq.getStatut())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Dossier clôturé ou archivé"));
        }

        // Contrainte : faute uniquement si niveau === 0 et pas déjà une faute enregistrée
        if (paq.getNiveau() != 0) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Une faute ne peut être enregistrée qu'avant le premier entretien (niveau 0)"));
        }
        if (paq.getDerniereFaute() != null) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Une faute a déjà été enregistrée. Veuillez procéder à l'entretien explicatif."));
        }

        String detailFaute = request.getDetail() != null ? request.getDetail() : "Faute professionnelle";

        // Niveau reste à 0 — c'est la faute qui déclenche l'entretien explicatif
        paq.setDerniereFaute(LocalDate.now());

        String historiqueMsg = String.format("Faute enregistrée - Détail: %s", detailFaute);
        paq.setHistorique(addHistorique(paq.getHistorique(),
                new HistoriqueEvent(LocalDate.now(), "Faute enregistrée", historiqueMsg)));

        PaqDossier saved = paqRepository.save(paq);
        return ResponseEntity.ok(Map.of(
                "paq", saved,
                "message", "Faute enregistrée. Veuillez lancer l'Entretien Explicatif."
        ));
    }

    // ─────────────────────────────────────────────
    // UPGRADE NIVEAU
    // ─────────────────────────────────────────────

    @PostMapping("/{matricule}/upgrade")
    public ResponseEntity<?> upgradeNiveau(@PathVariable String matricule) {
        Optional<PaqDossier> paqOpt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);
        if (paqOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Aucun dossier PAQ actif trouvé"));

        PaqDossier paq = paqOpt.get();

        if (paq.getNiveau() >= 5) return ResponseEntity.badRequest().body(Map.of("message", "Niveau maximum atteint"));
        if (paq.isArchived() || "CLOTURE".equals(paq.getStatut())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Dossier clôturé ou archivé"));
        }

        int ancienNiveau = paq.getNiveau();
        int nouveauNiveau = ancienNiveau + 1;
        paq.setNiveau(nouveauNiveau);

        if ("CRITIQUE".equals(paq.getStatut())) {
            paq.setStatut("EN_COURS");
        }

        paq.setHistorique(addHistorique(paq.getHistorique(),
                new HistoriqueEvent(LocalDate.now(), "Augmentation niveau",
                        String.format("Niveau passé de %d à %d", ancienNiveau, nouveauNiveau))));

        return ResponseEntity.ok(paqRepository.save(paq));
    }

    // ─────────────────────────────────────────────
    // ARCHIVER PAQ (manuel — disponible après 6 mois)
    // ─────────────────────────────────────────────

    /**
     * Archive manuellement un PAQ.
     * Le bouton n'est visible côté frontend que si dateFin <= aujourd'hui (6 mois écoulés).
     * L'archivage automatique est géré par ArchivingService / PaqSchedulerService.
     */
    @PostMapping("/{matricule}/archive")
    @Transactional
    public ResponseEntity<?> archivePaq(@PathVariable String matricule) {

        Optional<PaqDossier> opt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrue(matricule);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Aucun PAQ actif trouvé"));
        }

        PaqDossier paq = opt.get();

        // Vérifier que les 6 mois sont écoulés avant l'archivage manuel
        if (paq.getDateFin() != null && paq.getDateFin().isAfter(LocalDate.now())) {
            long joursRestants = ChronoUnit.DAYS.between(LocalDate.now(), paq.getDateFin());
            return ResponseEntity.badRequest().body(Map.of("message",
                    String.format("Le dossier ne peut être archivé qu'après la fin de la période (encore %d jours).", joursRestants)));
        }

        // Éviter doublon
        if (archiveRepository.existsByPaqDossierId(paq.getId())) {
            return ResponseEntity.ok(Map.of("message", "Déjà archivé"));
        }

        // Récupérer nom complet
        String nomPrenom = matricule;
        Optional<Collaborator> collabOpt = collaboratorRepository.findByMatricule(matricule);
        if (collabOpt.isPresent()) {
            Collaborator c = collabOpt.get();
            nomPrenom = c.getName() + " " + c.getPrenom();
        }

        // Ajouter événement d'archivage dans l'historique
        paq.setHistorique(addHistorique(paq.getHistorique(),
                new HistoriqueEvent(LocalDate.now(), "Archivage manuel",
                        "Dossier archivé manuellement après la période de 6 mois")));

        // Marquer le PAQ comme archivé
        paq.setArchived(true);
        paq.setActif(false);
        paq.setStatut("ARCHIVE");
        paq.setDateArchivage(LocalDate.now());
        paqRepository.save(paq);

        // Créer l'entrée Archive enrichie
        createArchiveEntry(paq, nomPrenom);

        return ResponseEntity.ok(Map.of("message", "PAQ archivé avec succès"));
    }



    @PostMapping("/{matricule}/premier-entretien")
    public ResponseEntity<?> createPremierEntretien(@PathVariable String matricule, @RequestBody EntretienRequest request) {
        return createEntretien(matricule, 1, request, "Premier entretien");
    }

    @PostMapping("/{matricule}/deuxieme-entretien")
    public ResponseEntity<?> createDeuxiemeEntretien(@PathVariable String matricule, @RequestBody EntretienRequest request) {
        return createEntretien(matricule, 2, request, "Deuxième entretien");
    }

    @PostMapping("/{matricule}/troisieme-entretien")
    public ResponseEntity<?> createTroisiemeEntretien(@PathVariable String matricule, @RequestBody EntretienRequest request) {
        return createEntretien(matricule, 3, request, "Troisième entretien");
    }

    @PostMapping("/{matricule}/quatrieme-entretien")
    public ResponseEntity<?> createQuatriemeEntretien(@PathVariable String matricule, @RequestBody EntretienRequest request) {
        return createEntretien(matricule, 4, request, "Quatrième entretien");
    }

    @PostMapping("/{matricule}/cinquieme-entretien")
    public ResponseEntity<?> createCinquiemeEntretien(@PathVariable String matricule, @RequestBody EntretienRequest request) {
        return createEntretien(matricule, 5, request, "Cinquième entretien - Clôture");
    }

    private ResponseEntity<?> createEntretien(String matricule, int niveau, EntretienRequest request, String entretienNom) {
        Optional<PaqDossier> paqOpt = paqRepository.findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(matricule);
        if (paqOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Aucun dossier PAQ actif trouvé"));

        PaqDossier paq = paqOpt.get();

        if (paq.isArchived() || "CLOTURE".equals(paq.getStatut())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Dossier clôturé ou archivé"));
        }
        if (paq.getNiveau() >= niveau) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cet entretien a déjà été réalisé"));
        }
        if (paq.getNiveau() != niveau - 1) {
            return ResponseEntity.badRequest().body(Map.of("message", "Veuillez réaliser les entretiens dans l'ordre"));
        }

        // Pour le premier entretien (niveau 1), une faute doit avoir été déclarée
        if (niveau == 1 && paq.getDerniereFaute() == null) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Une faute doit être enregistrée avant le premier entretien"));
        }

        LocalDate entretienDate = request.getDate() != null ? request.getDate() : LocalDate.now();
        String notes = request.getNotes() != null ? request.getNotes() : "";

        switch (niveau) {
            case 1 -> { paq.setDatePremierEntretien(entretienDate); paq.setPremierEntretienNotes(notes); }
            case 2 -> { paq.setDateDeuxiemeEntretien(entretienDate); paq.setDeuxiemeEntretienNotes(notes); }
            case 3 -> { paq.setDateTroisiemeEntretien(entretienDate); paq.setTroisiemeEntretienNotes(notes); }
            case 4 -> { paq.setDateQuatriemeEntretien(entretienDate); paq.setQuatriemeEntretienNotes(notes); }
            case 5 -> {
                paq.setDateCinquiemeEntretien(entretienDate);
                paq.setCinquiemeEntretienNotes(notes);
                paq.setStatut("CLOTURE");
            }
        }

        paq.setNiveau(niveau);

        String detail = String.format("%s réalisé le %s", entretienNom, entretienDate);
        if (!notes.isEmpty()) detail += " - Notes: " + notes;

        paq.setHistorique(addHistorique(paq.getHistorique(),
                new HistoriqueEvent(LocalDate.now(), entretienNom, detail)));

        return ResponseEntity.ok(paqRepository.save(paq));
    }
}