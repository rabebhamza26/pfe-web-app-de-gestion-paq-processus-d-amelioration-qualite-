package com.polytech.paqbackend.repository;

import com.polytech.paqbackend.entity.EntretienMesure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
@Repository

public interface EntretienMesureRepository  extends JpaRepository<EntretienMesure, Long> {
    List<EntretienMesure> findByMatricule(String matricule);


    @Query("SELECT COUNT(e) FROM EntretienMesure e WHERE e.matricule IN :matricules")
    long countByMatriculeIn(@Param("matricules") List<String> matricules);

    @Query("SELECT e FROM EntretienMesure e WHERE e.matricule IN :matricules")
    List<EntretienMesure> findByMatriculeIn(@Param("matricules") List<String> matricules);

    // CORRECTION: Utiliser dateCreation
    @Query("SELECT COUNT(e) FROM EntretienMesure e WHERE e.matricule IN :matricules " +
            "AND e.dateCreation BETWEEN :startDate AND :endDate")
    long countByMatriculeInAndDateBetween(@Param("matricules") List<String> matricules,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
}