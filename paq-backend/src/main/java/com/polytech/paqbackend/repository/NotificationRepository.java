package com.polytech.paqbackend.repository;

import com.polytech.paqbackend.entity.Notification;
import com.polytech.paqbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByDestinataireLoginOrderByCreatedAtDesc(String destinataireLogin);

    List<Notification> findByDestinataireLoginAndLuOrderByCreatedAtDesc(String destinataireLogin, boolean lu);

    long countByDestinataireLoginAndLu(String destinataireLogin, boolean lu);

    Optional<Notification> findByIdAndDestinataireLogin(Long id, String destinataireLogin);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.lu = true WHERE n.destinataireLogin = :login AND n.lu = false")
    void markAllAsRead(@Param("login") String login);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.lu = true WHERE n.id = :id")
    void markAsRead(@Param("id") Long id);

}