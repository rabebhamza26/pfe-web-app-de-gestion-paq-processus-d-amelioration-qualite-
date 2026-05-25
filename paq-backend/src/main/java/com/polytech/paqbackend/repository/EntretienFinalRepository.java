package com.polytech.paqbackend.repository;

import com.polytech.paqbackend.entity.EntretienExplicatif;
import com.polytech.paqbackend.entity.EntretienFinal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EntretienFinalRepository extends JpaRepository<EntretienFinal, Long> {

    /**
     * Récupère tous les entretiens finaux d'un collaborateur, triés par date.
     */
    List<EntretienFinal> findByMatricule(String matricule);

    List<EntretienFinal> findByMatriculeOrderByDateEntretienDesc(String matricule);

    @Query("SELECT COUNT(e) FROM EntretienFinal e WHERE e.matricule IN :matricules")
    long countByMatriculeIn(@Param("matricules") List<String> matricules);

    @Query("SELECT e FROM EntretienFinal e WHERE e.matricule IN :matricules")
    List<EntretienFinal> findByMatriculeIn(@Param("matricules") List<String> matricules);

    // CORRECTION: Utiliser dateEntretien
    @Query("SELECT COUNT(e) FROM EntretienFinal e WHERE e.matricule IN :matricules " +
            "AND e.dateEntretien BETWEEN :startDate AND :endDate")
    long countByMatriculeInAndDateBetween(@Param("matricules") List<String> matricules,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
}