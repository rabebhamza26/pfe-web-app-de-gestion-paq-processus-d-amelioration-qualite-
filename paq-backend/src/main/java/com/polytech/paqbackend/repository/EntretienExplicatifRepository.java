package com.polytech.paqbackend.repository;

import com.polytech.paqbackend.entity.EntretienExplicatif;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository

public interface EntretienExplicatifRepository extends JpaRepository<EntretienExplicatif, Long> {
    List<EntretienExplicatif> findByMatricule(String matricule);
    @Query("SELECT COUNT(e) FROM EntretienExplicatif e WHERE e.matricule IN :matricules")
    long countByMatriculeIn(@Param("matricules") List<String> matricules);

    @Query("SELECT e FROM EntretienExplicatif e WHERE e.matricule IN :matricules")
    List<EntretienExplicatif> findByMatriculeIn(@Param("matricules") List<String> matricules);

    // CORRECTION: Utiliser createdAt qui est un LocalDateTime
    @Query("SELECT COUNT(e) FROM EntretienExplicatif e WHERE e.matricule IN :matricules " +
            "AND DATE(e.createdAt) BETWEEN :startDate AND :endDate")
    long countByMatriculeInAndDateBetween(@Param("matricules") List<String> matricules,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

}
