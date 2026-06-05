package com.polytech.paqbackend.repository;

import com.polytech.paqbackend.entity.PaqDossier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaqRepository extends JpaRepository<PaqDossier, Long> {
    Optional<PaqDossier> findFirstByCollaboratorMatriculeAndActifTrue(String matricule);

    Optional<PaqDossier> findFirstByCollaboratorMatriculeAndActifTrueAndArchivedFalse(String matricule);

    List<PaqDossier> findAllByCollaboratorMatriculeOrderByDateCreationDesc(String matricule);

    List<PaqDossier> findByActifTrueAndArchivedFalse();


    boolean existsByCollaboratorMatriculeAndActifTrueAndArchivedFalse(String matricule);

    @Query("SELECT p FROM PaqDossier p WHERE p.collaboratorMatricule = :matricule AND p.dateFin < :date AND p.archived = false")
    List<PaqDossier> findExpiredPaqs(@Param("matricule") String matricule, @Param("date") LocalDate date);

    // Add this method with @Query
    @Query("SELECT p FROM PaqDossier p WHERE p.createdAt < :date AND p.statut != :statut")
    List<PaqDossier> findOldPaqsNotArchived(@Param("date") LocalDateTime date, @Param("statut") String statut);

    List<PaqDossier> findByCollaboratorMatriculeOrderByDateCreationDesc(String matricule);

    Optional<PaqDossier> findByCollaboratorMatricule(String matricule);


    @Query("SELECT p FROM PaqDossier p WHERE p.archived = false")
    List<PaqDossier> findByArchivedFalse();

    // Alternative si la méthode ci-dessus ne fonctionne pas
    default List<PaqDossier> findAllNonArchived() {
        return findAll().stream()
                .filter(p -> !p.isArchived())
                .collect(java.util.stream.Collectors.toList());
    }

}

