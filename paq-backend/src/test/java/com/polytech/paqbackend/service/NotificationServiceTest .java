package com.polytech.paqbackend.service;

import com.polytech.paqbackend.entity.Notification;
import com.polytech.paqbackend.entity.User;
import com.polytech.paqbackend.repository.CollaboratorRepository;
import com.polytech.paqbackend.repository.NotificationRepository;
import com.polytech.paqbackend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository repo;
    @Mock private UserRepository userRepository;
    @Mock private CollaboratorRepository collaboratorRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void shouldEnvoyerNotification() {
        User destinataire = new User();
        destinataire.setLogin("userLogin");
        destinataire.setEmail("user@test.com");
        when(userRepository.findByLogin("userLogin")).thenReturn(destinataire);
        when(repo.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        Notification result = notificationService.envoyerNotification("userLogin", "Titre", "Message", "INFO", "M001", "EXPLICATIF");

        assertThat(result).isNotNull();
        assertThat(result.getTitre()).isEqualTo("Titre");
        verify(messagingTemplate).convertAndSendToUser(eq("userLogin"), eq("/queue/notifications"), any());
    }
}