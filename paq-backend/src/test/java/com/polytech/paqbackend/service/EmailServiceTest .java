package com.polytech.paqbackend.service;

import com.polytech.paqbackend.entity.User;
import com.polytech.paqbackend.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private NotificationService notificationService;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private EmailService emailService;

    @Test
    void shouldSendEmail() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        Authentication auth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(auth.getName()).thenReturn("test@example.com");
        User user = new User();
        user.setLogin("testLogin");
        when(userRepository.findByEmailOrLogin("test@example.com")).thenReturn(user);

        emailService.sendEmail("exp@test.com", "dest@test.com", "Sujet", "<html>Contenu</html>");

        verify(mailSender).send(any(MimeMessage.class));
        verify(notificationService).envoyerNotification(eq("testLogin"), anyString(), anyString(), anyString());
    }
}