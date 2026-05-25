package com.polytech.paqbackend.service;

import com.polytech.paqbackend.entity.Collaborator;
import com.polytech.paqbackend.entity.PaqDossier;
import com.polytech.paqbackend.repository.CollaboratorRepository;
import com.polytech.paqbackend.repository.PaqRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CollaboratorService {

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private PaqRepository paqRepository;

    // QualificationService complètement supprimé

    @Transactional
    public Collaborator create(Collaborator collaborator) {
        // Initialisation des valeurs par défaut
        collaborator.setNiveau(0);
        collaborator.setStatus("ACTIF");
        collaborator.setActif(true);
        collaborator.setArchived(false);
        collaborator.setDepart(false);
        collaborator.setDepartDate(null); // Initialiser à null

        // Sauvegarde du collaborateur
        Collaborator saved = collaboratorRepository.save(collaborator);

        return saved;
    }

    @Transactional
    public Collaborator update(String matricule, Collaborator collaborator) {
        Optional<Collaborator> existingOpt = collaboratorRepository.findById(matricule);
        if (!existingOpt.isPresent()) {
            throw new RuntimeException("Collaborateur non trouvé");
        }

        Collaborator existing = existingOpt.get();

        if (collaborator.getName() != null && !collaborator.getName().trim().isEmpty()) {
            existing.setName(collaborator.getName());
        }
        if (collaborator.getPrenom() != null && !collaborator.getPrenom().trim().isEmpty()) {
            existing.setPrenom(collaborator.getPrenom());
        }
        if (collaborator.getSegment() != null && !collaborator.getSegment().trim().isEmpty()) {
            existing.setSegment(collaborator.getSegment());
        }
        if (collaborator.getHireDate() != null) {
            existing.setHireDate(collaborator.getHireDate());
        }

        Collaborator updated = collaboratorRepository.save(existing);

        return updated;
    }

    @Transactional
    public void delete(String matricule) {
        if (!collaboratorRepository.existsById(matricule)) {
            throw new RuntimeException("Collaborateur non trouvé");
        }

        // Archiver les dossiers PAQ associés
        Optional<PaqDossier> activePaq = paqRepository.findFirstByCollaboratorMatriculeAndActifTrue(matricule);
        if (activePaq.isPresent()) {
            PaqDossier paq = activePaq.get();
            paq.setArchived(true);
            paq.setActif(false);
            paq.setStatut("ARCHIVE");
            paqRepository.save(paq);
        }

        collaboratorRepository.deleteById(matricule);
    }

    // Méthode utilitaire pour vérifier l'existence d'un collaborateur
    public boolean existsByMatricule(String matricule) {
        return collaboratorRepository.existsById(matricule);
    }

    // Méthode pour trouver un collaborateur par matricule
    public Optional<Collaborator> findByMatricule(String matricule) {
        return collaboratorRepository.findById(matricule);
    }
}