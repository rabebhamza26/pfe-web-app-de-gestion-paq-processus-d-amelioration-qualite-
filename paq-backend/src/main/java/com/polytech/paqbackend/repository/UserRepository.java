package com.polytech.paqbackend.repository;

import com.polytech.paqbackend.dto.SiteUserDistributionDTO;
import com.polytech.paqbackend.entity.Role;
import com.polytech.paqbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {


    long countByActiveFalse();
    User findByLogin(String login);
    long countByActiveTrue();
    long countByCreatedAtAfter(LocalDateTime dateTime);
    User findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);


    @Query("select u.role, count(u) from User u group by u.role")
    List<Object[]> countUsersByRole();

    @Query("SELECT u.email FROM User u WHERE u.email IS NOT NULL AND u.email != ''")
    List<String> findAllEmails();

    // Requêtes avec jointures pour récupérer toutes les relations
    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.sites " +
            "LEFT JOIN FETCH u.plants " +
            "LEFT JOIN FETCH u.segments " +
            "WHERE u.id = :id")
    Optional<User> findByIdWithAllRelations(@Param("id") Long id);

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.sites " +
            "LEFT JOIN FETCH u.plants " +
            "LEFT JOIN FETCH u.segments")
    List<User> findAllWithAllRelations();

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.segments " +
            "LEFT JOIN FETCH u.plants " +
            "LEFT JOIN FETCH u.sites " +
            "WHERE u.email = :username OR u.login = :username")
    User findByEmailOrLoginWithPerimeter(@Param("username") String username);

    @Query("SELECT u FROM User u WHERE u.email = :username OR u.login = :username")
    User findByEmailOrLogin(@Param("username") String username);

    @Query("SELECT u FROM User u WHERE u.role = 'SL'")
    List<User> findAllSL();

    @Query("SELECT u.email FROM User u WHERE u.active = true AND u.email IS NOT NULL AND u.email != ''")
    List<String> findAllActiveUserEmails();

    // ⭐ NOUVEAU: Récupérer les emails par Site ET Plant
    @Query("SELECT DISTINCT u.email FROM User u " +
            "JOIN u.sites s " +
            "JOIN u.plants p " +
            "WHERE s.id = :siteId AND p.id = :plantId " +
            "AND u.active = true " +
            "AND u.email IS NOT NULL AND u.email != ''")
    List<String> findEmailsBySiteAndPlant(@Param("siteId") Long siteId, @Param("plantId") Long plantId);

    // Récupérer les emails par Site uniquement
    @Query("SELECT DISTINCT u.email FROM User u " +
            "JOIN u.sites s " +
            "WHERE s.id = :siteId " +
            "AND u.active = true " +
            "AND u.email IS NOT NULL AND u.email != ''")
    List<String> findEmailsBySite(@Param("siteId") Long siteId);

    // Récupérer les emails par Plant uniquement
    @Query("SELECT DISTINCT u.email FROM User u " +
            "JOIN u.plants p " +
            "WHERE p.id = :plantId " +
            "AND u.active = true " +
            "AND u.email IS NOT NULL AND u.email != ''")
    List<String> findEmailsByPlant(@Param("plantId") Long plantId);




// Cherche les users SGL dont un de leurs segments a ce nomSegment

    @Query("""
    SELECT u FROM User u
    JOIN u.segments s
    WHERE u.role = 'SGL'
      AND s.nomSegment = :segmentNom
""")
    List<User> findSGLBySegmentNom(@Param("segmentNom") String segmentNom);





    // Retourne une List<User> car plusieurs SL peuvent exister pour un même segment
    @Query("SELECT u FROM User u JOIN u.segments s WHERE u.role = :role AND s.nomSegment = :segment")
    List<User> findByRoleAndSegment(@Param("role") String role, @Param("segment") String segment);


    // ⭐ AJOUTER CETTE MÉTHODE - Récupérer les utilisateurs par rôle
    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findByRole(@Param("role") Role role);

    // ⭐ Version alternative avec le nom du rôle en String
    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findByRoleName(@Param("role") Role role);

    // Récupérer les emails des QM_SEGMENT uniquement
    @Query("SELECT u.email FROM User u WHERE u.role = 'QM_SEGMENT' AND u.active = true AND u.email IS NOT NULL AND u.email != ''")
    List<String> findQMEmails();

    // Dans UserRepository.java - Ajoutez ces méthodes

    // Récupérer les emails QM_SEGMENT par Site
    @Query("SELECT DISTINCT u.email FROM User u " +
            "JOIN u.sites s " +
            "WHERE u.role = 'QM_SEGMENT' " +
            "AND u.active = true " +
            "AND s.id = :siteId " +
            "AND u.email IS NOT NULL AND u.email != ''")
    List<String> findQMEmailsBySite(@Param("siteId") Long siteId);

    // Récupérer les emails QM_SEGMENT par Plant
    @Query("SELECT DISTINCT u.email FROM User u " +
            "JOIN u.plants p " +
            "WHERE u.role = 'QM_SEGMENT' " +
            "AND u.active = true " +
            "AND p.id = :plantId " +
            "AND u.email IS NOT NULL AND u.email != ''")
    List<String> findQMEmailsByPlant(@Param("plantId") Long plantId);


    // Récupérer les emails SGL par Site
    @Query("SELECT DISTINCT u.email FROM User u " +
            "JOIN u.sites s " +
            "WHERE u.role = 'SGL' " +
            "AND u.active = true " +
            "AND s.id = :siteId " +
            "AND u.email IS NOT NULL AND u.email != ''")
    List<String> findSGLEmailsBySite(@Param("siteId") Long siteId);

    // Récupérer les emails SGL par Plant
    @Query("SELECT DISTINCT u.email FROM User u " +
            "JOIN u.plants p " +
            "WHERE u.role = 'SGL' " +
            "AND u.active = true " +
            "AND p.id = :plantId " +
            "AND u.email IS NOT NULL AND u.email != ''")
    List<String> findSGLEmailsByPlant(@Param("plantId") Long plantId);

    // Récupérer tous les emails SGL
    @Query("SELECT u.email FROM User u WHERE u.role = 'SGL' AND u.active = true AND u.email IS NOT NULL AND u.email != ''")
    List<String> findSGLEmails();



    // HP emails

    @Query("SELECT u.email FROM User u WHERE u.role = 'HP' AND u.active = true AND u.email IS NOT NULL AND u.email != ''")
    List<String> findHPEmails();

    @Query("SELECT DISTINCT u.email FROM User u " +
            "JOIN u.sites s " +
            "WHERE u.role = 'HP' " +
            "AND u.active = true " +
            "AND s.id = :siteId " +
            "AND u.email IS NOT NULL AND u.email != ''")
    List<String> findHPEmailsBySite(@Param("siteId") Long siteId);

    @Query("SELECT DISTINCT u.email FROM User u " +
            "JOIN u.plants p " +
            "WHERE u.role = 'HP' " +
            "AND u.active = true " +
            "AND p.id = :plantId " +
            "AND u.email IS NOT NULL AND u.email != ''")
    List<String> findHPEmailsByPlant(@Param("plantId") Long plantId);

    // ========== QM_PLANT ==========
    @Query("SELECT u.email FROM User u WHERE u.role = 'QM_PLANT' AND u.active = true AND u.email IS NOT NULL AND u.email != ''")
    List<String> findQMPlantEmails();

    @Query("SELECT DISTINCT u.email FROM User u " +
            "JOIN u.sites s " +
            "WHERE u.role = 'QM_PLANT' " +
            "AND u.active = true " +
            "AND s.id = :siteId " +
            "AND u.email IS NOT NULL AND u.email != ''")
    List<String> findQMPlantEmailsBySite(@Param("siteId") Long siteId);

    @Query("SELECT DISTINCT u.email FROM User u " +
            "JOIN u.plants p " +
            "WHERE u.role = 'QM_PLANT' " +
            "AND u.active = true " +
            "AND p.id = :plantId " +
            "AND u.email IS NOT NULL AND u.email != ''")
    List<String> findQMPlantEmailsByPlant(@Param("plantId") Long plantId);






}