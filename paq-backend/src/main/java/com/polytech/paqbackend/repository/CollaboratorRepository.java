package com.polytech.paqbackend.repository;

import com.polytech.paqbackend.dto.CollaborateurDTO;
import com.polytech.paqbackend.entity.Collaborator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollaboratorRepository extends JpaRepository<Collaborator, String> {

    @Query("SELECT c FROM Collaborator c " +
            "WHERE c.niveau = :niveau " +
            "AND c.actif = :actif " +
            "AND c.depart = false " +
            "AND c.archived = false " +
            "AND c.hireDate <= :date")
    List<Collaborator> findAllByNiveauAndActifAndHireDateBefore(
            @Param("niveau") int niveau,
            @Param("actif") boolean actif,
            @Param("date") LocalDate date
    );
    boolean existsByMatricule(String matricule);

    long countByActifTrue();

    Optional<Collaborator> findByMatricule(String matricule);

    @Query("SELECT c FROM Collaborator c WHERE c.actif = true AND c.depart = false")
    List<Collaborator> findAllActive();

    @Query("SELECT c FROM Collaborator c WHERE c.depart = false AND c.archived = false")
    List<Collaborator> findByDepartFalseAndArchivedFalse();

    @Query("SELECT c FROM Collaborator c WHERE c.depart = true AND c.archived = false")
    List<Collaborator> findByDepartTrueAndArchivedFalse();

    // =========================================================
    // NOUVELLES REQUÊTES : résolution segment ↔ plant/site
    // =========================================================

    /**
     * Retourne les noms de segments appartenant à un plant donné.
     * Utilisé pour filtrer les collaborateurs quand le frontend
     * envoie plantId en paramètre.
     */
    @Query("SELECT s.nomSegment FROM Segment s WHERE s.plant.id = :plantId")
    List<String> findSegmentNamesByPlantId(@Param("plantId") Long plantId);

    /**
     * Retourne les noms de segments appartenant à un site donné
     * (via plant → site).
     * Utilisé pour filtrer les collaborateurs quand le frontend
     * envoie siteId en paramètre.
     */
    @Query("SELECT s.nomSegment FROM Segment s WHERE s.plant.site.id = :siteId")
    List<String> findSegmentNamesBySiteId(@Param("siteId") Long siteId);

    // =========================================================
    // REQUÊTES EXISTANTES
    // =========================================================

    /**
     * Récupère tous les collaborateurs avec leurs informations PAQ (version avec paramètre)
     */
    @Query("""
        SELECT new com.polytech.paqbackend.dto.CollaborateurDTO(
            c.matricule,
            c.name,
            c.prenom,
            c.segment,
            c.hireDate,
            COALESCE(p.niveau, 0),
            p.derniereFaute,
            CASE 
                WHEN p.id IS NULL THEN 'POSITIF'
                WHEN p.statut = 'CLOTURE' THEN 'CLOTURE'
                WHEN p.statut = 'CRITIQUE' THEN 'CRITIQUE'
                ELSE CONCAT('NIVEAU ', p.niveau)
            END,
            CASE 
                WHEN p.id IS NULL 
                     AND c.hireDate <= :sixMonthsAgo 
                THEN true
                ELSE false
            END
        )
        FROM Collaborator c
        LEFT JOIN PaqDossier p ON c.matricule = p.collaboratorMatricule AND p.actif = true AND p.archived = false
        WHERE c.archived = false AND c.depart = false
        ORDER BY c.matricule
    """)
    List<CollaborateurDTO> getAllWithPaqWithDate(@Param("sixMonthsAgo") LocalDate sixMonthsAgo);

    /**
     * Récupère tous les collaborateurs avec leurs informations PAQ (version sans paramètre)
     */
    default List<CollaborateurDTO> getAllWithPaq() {
        return getAllWithPaqWithDate(LocalDate.now().minusMonths(6));
    }

    /**
     * Récupère les collaborateurs sans faute
     */
    @Query("""
        SELECT new com.polytech.paqbackend.dto.CollaborateurDTO(
            c.matricule,
            c.name,
            c.prenom,
            c.segment,
            c.hireDate,
            COALESCE(p.niveau, 0),
            p.derniereFaute,
            CASE 
                WHEN p.id IS NULL THEN 'POSITIF'
                WHEN p.statut = 'CLOTURE' THEN 'CLOTURE'
                WHEN p.statut = 'CRITIQUE' THEN 'CRITIQUE'
                ELSE CONCAT('NIVEAU ', p.niveau)
            END,
            CASE 
                WHEN p.id IS NULL 
                     AND c.hireDate <= :sixMonthsAgo 
                THEN true
                ELSE false
            END
        )
        FROM Collaborator c
        LEFT JOIN PaqDossier p ON c.matricule = p.collaboratorMatricule AND p.actif = true AND p.archived = false
        WHERE c.archived = false 
          AND c.depart = false
          AND (p.id IS NULL OR p.niveau = 0)
        ORDER BY c.matricule
    """)
    List<CollaborateurDTO> findSansFauteWithDate(@Param("sixMonthsAgo") LocalDate sixMonthsAgo);

    default List<CollaborateurDTO> findSansFaute() {
        return findSansFauteWithDate(LocalDate.now().minusMonths(6));
    }

    @Query("SELECT c.segment, COUNT(c) FROM Collaborator c WHERE c.archived = false AND c.depart = false GROUP BY c.segment")
    List<Object[]> countBySegment();

    @Query("SELECT c FROM Collaborator c WHERE c.segment = :segment AND c.archived = false AND c.depart = false")
    List<Collaborator> findBySegment(@Param("segment") String segment);

    @Query("""
        SELECT c FROM Collaborator c 
        WHERE c.matricule IN (
            SELECT p.collaboratorMatricule FROM PaqDossier p 
            WHERE p.niveau = :niveau AND p.actif = true AND p.archived = false
        )
        AND c.archived = false AND c.depart = false
    """)
    List<Collaborator> findByPaqNiveau(@Param("niveau") int niveau);

    // ========== MÉTHODES DE FILTRAGE PAR SEGMENT / SITE / PLANT ==========

    /**
     * Récupère les collaborateurs filtrés par liste de segments.
     * C'est la requête principale utilisée par /view avec filtre.
     */
    @Query("""
        SELECT new com.polytech.paqbackend.dto.CollaborateurDTO(
            c.matricule,
            c.name,
            c.prenom,
            c.segment,
            c.hireDate,
            COALESCE(p.niveau, 0),
            p.derniereFaute,
            CASE 
                WHEN p.id IS NULL THEN 'POSITIF'
                WHEN p.statut = 'CLOTURE' THEN 'CLOTURE'
                WHEN p.statut = 'CRITIQUE' THEN 'CRITIQUE'
                ELSE CONCAT('NIVEAU ', p.niveau)
            END,
            CASE 
                WHEN p.id IS NULL 
                     AND c.hireDate <= :sixMonthsAgo 
                THEN true
                ELSE false
            END
        )
        FROM Collaborator c
        LEFT JOIN PaqDossier p ON c.matricule = p.collaboratorMatricule AND p.actif = true AND p.archived = false
        WHERE c.archived = false 
          AND c.depart = false
          AND c.segment IN :segments
        ORDER BY c.matricule
    """)
    List<CollaborateurDTO> getCollaboratorsBySegmentsWithDate(
            @Param("segments") List<String> segments,
            @Param("sixMonthsAgo") LocalDate sixMonthsAgo);

    default List<CollaborateurDTO> getCollaboratorsBySegments(List<String> segments) {
        return getCollaboratorsBySegmentsWithDate(segments, LocalDate.now().minusMonths(6));
    }

    /**
     * Récupère les collaborateurs par segment unique.
     */
    @Query("""
        SELECT new com.polytech.paqbackend.dto.CollaborateurDTO(
            c.matricule,
            c.name,
            c.prenom,
            c.segment,
            c.hireDate,
            COALESCE(p.niveau, 0),
            p.derniereFaute,
            CASE 
                WHEN p.id IS NULL THEN 'POSITIF'
                WHEN p.statut = 'CLOTURE' THEN 'CLOTURE'
                WHEN p.statut = 'CRITIQUE' THEN 'CRITIQUE'
                ELSE CONCAT('NIVEAU ', p.niveau)
            END,
            p.id IS NOT NULL
        )
        FROM Collaborator c
        LEFT JOIN PaqDossier p ON c.matricule = p.collaboratorMatricule AND p.actif = true AND p.archived = false
        WHERE c.archived = false AND c.depart = false
          AND (:segment IS NULL OR c.segment = :segment)
        ORDER BY c.matricule
    """)
    List<CollaborateurDTO> getCollaboratorsBySegment(@Param("segment") String segment);

    /**
     * Récupère les collaborateurs par site (via segment → plant → site).
     */
    @Query("""
        SELECT new com.polytech.paqbackend.dto.CollaborateurDTO(
            c.matricule,
            c.name,
            c.prenom,
            c.segment,
            c.hireDate,
            COALESCE(p.niveau, 0),
            p.derniereFaute,
            CASE 
                WHEN p.id IS NULL THEN 'POSITIF'
                WHEN p.statut = 'CLOTURE' THEN 'CLOTURE'
                WHEN p.statut = 'CRITIQUE' THEN 'CRITIQUE'
                ELSE CONCAT('NIVEAU ', p.niveau)
            END,
            p.id IS NOT NULL
        )
        FROM Collaborator c
        LEFT JOIN PaqDossier p ON c.matricule = p.collaboratorMatricule AND p.actif = true AND p.archived = false
        WHERE c.archived = false AND c.depart = false
          AND c.segment IN (
              SELECT s.nomSegment FROM Segment s WHERE s.plant.site.id IN :siteIds
          )
        ORDER BY c.matricule
    """)
    List<CollaborateurDTO> getCollaboratorsBySites(@Param("siteIds") List<Long> siteIds);

    /**
     * Récupère les collaborateurs par plant (via segment → plant).
     */
    @Query("""
        SELECT new com.polytech.paqbackend.dto.CollaborateurDTO(
            c.matricule,
            c.name,
            c.prenom,
            c.segment,
            c.hireDate,
            COALESCE(p.niveau, 0),
            p.derniereFaute,
            CASE 
                WHEN p.id IS NULL THEN 'POSITIF'
                WHEN p.statut = 'CLOTURE' THEN 'CLOTURE'
                WHEN p.statut = 'CRITIQUE' THEN 'CRITIQUE'
                ELSE CONCAT('NIVEAU ', p.niveau)
            END,
            p.id IS NOT NULL
        )
        FROM Collaborator c
        LEFT JOIN PaqDossier p ON c.matricule = p.collaboratorMatricule AND p.actif = true AND p.archived = false
        WHERE c.archived = false AND c.depart = false
          AND c.segment IN (
              SELECT s.nomSegment FROM Segment s WHERE s.plant.id IN :plantIds
          )
        ORDER BY c.matricule
    """)
    List<CollaborateurDTO> getCollaboratorsByPlants(@Param("plantIds") List<Long> plantIds);


    @Query("SELECT c FROM Collaborator c WHERE c.hireDate <= :date AND c.actif = true AND c.depart = false AND c.archived = false")
    List<Collaborator> findActiveCollaboratorsByHireDateBefore(@Param("date") LocalDate date);
}