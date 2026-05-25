package com.polytech.paqbackend.repository;


import com.polytech.paqbackend.entity.EntretienDaccord;
import com.polytech.paqbackend.entity.EntretienExplicatif;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
@Repository

public interface EntretienDaccordRepository extends JpaRepository<EntretienDaccord, Long> {
    List<EntretienDaccord> findByMatricule(String matricule);

    @Query("SELECT COUNT(e) FROM EntretienDaccord e WHERE e.matricule IN :matricules")
    long countByMatriculeIn(@Param("matricules") List<String> matricules);

    @Query("SELECT e FROM EntretienDaccord e WHERE e.matricule IN :matricules")
    List<EntretienDaccord> findByMatriculeIn(@Param("matricules") List<String> matricules);

    // CORRECTION: Utiliser createdAt
    @Query("SELECT COUNT(e) FROM EntretienDaccord e WHERE e.matricule IN :matricules " +
            "AND DATE(e.createdAt) BETWEEN :startDate AND :endDate")
    long countByMatriculeInAndDateBetween(@Param("matricules") List<String> matricules,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);


}
