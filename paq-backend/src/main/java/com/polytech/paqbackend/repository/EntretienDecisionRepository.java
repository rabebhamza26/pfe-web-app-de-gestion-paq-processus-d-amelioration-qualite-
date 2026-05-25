package com.polytech.paqbackend.repository;

import com.polytech.paqbackend.entity.EntretienDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository

public interface EntretienDecisionRepository extends JpaRepository<EntretienDecision, Long> {

    List<EntretienDecision> findByMatricule(String matricule);


    @Query("SELECT COUNT(e) FROM EntretienDecision e WHERE e.matricule IN :matricules")
    long countByMatriculeIn(@Param("matricules") List<String> matricules);

    @Query("SELECT e FROM EntretienDecision e WHERE e.matricule IN :matricules")
    List<EntretienDecision> findByMatriculeIn(@Param("matricules") List<String> matricules);

    // CORRECTION: Utiliser dateCreation qui est un LocalDate
    @Query("SELECT COUNT(e) FROM EntretienDecision e WHERE e.matricule IN :matricules " +
            "AND e.dateCreation BETWEEN :startDate AND :endDate")
    long countByMatriculeInAndDateBetween(@Param("matricules") List<String> matricules,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
}