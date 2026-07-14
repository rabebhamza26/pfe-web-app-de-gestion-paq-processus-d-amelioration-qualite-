package com.polytech.paqbackend.service;

import com.polytech.paqbackend.dto.PasswordResetDto;
import com.polytech.paqbackend.dto.PasswordResetRequestDto;
import com.polytech.paqbackend.dto.PasswordResetResponseDto;
import com.polytech.paqbackend.entity.PasswordResetToken;
import com.polytech.paqbackend.entity.User;
import com.polytech.paqbackend.repository.PasswordResetTokenRepository;
import com.polytech.paqbackend.repository.UserRepository;
import com.polytech.paqbackend.token.TokenRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository resetTokenRepository;
    @Mock private TokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JavaMailSender mailSender;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User user;
    private PasswordResetRequestDto requestDto;
    private PasswordResetDto resetDto;
    private PasswordResetToken resetToken;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .login("testLogin")
                .nomUtilisateur("Test User")
                .active(true)
                .password("encodedPassword")
                .build();

        requestDto = new PasswordResetRequestDto();
        requestDto.setEmail("test@example.com");

        resetDto = new PasswordResetDto();
        resetDto.setToken("valid-token");
        resetDto.setNewPassword("newPassword123");
        resetDto.setConfirmPassword("newPassword123");

        resetToken = PasswordResetToken.builder()
                .token("valid-token")
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
    }

    @Test
    void shouldSendPasswordResetEmailWhenUserExists() {
        // ✅ Correction : appeler la méthode sur le mock, pas sur Optional
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(user));
        when(resetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(resetToken);
        // Spécifier le type MimeMessage pour lever l'ambiguïté
        doNothing().when(mailSender).send(any(MimeMessage.class));

        PasswordResetResponseDto response = passwordResetService.sendPasswordResetEmail(requestDto);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("Un email de réinitialisation a été envoyé");
        verify(userRepository).findByEmailIgnoreCase("test@example.com");
        verify(resetTokenRepository).save(any(PasswordResetToken.class));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void shouldReturnSuccessWhenUserNotFound() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        PasswordResetResponseDto response = passwordResetService.sendPasswordResetEmail(requestDto);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("Si un compte existe avec cet email");
        verify(userRepository).findByEmailIgnoreCase(anyString());
        verify(resetTokenRepository, never()).save(any());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void shouldReturnErrorWhenEmailIsEmpty() {
        requestDto.setEmail("");

        PasswordResetResponseDto response = passwordResetService.sendPasswordResetEmail(requestDto);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("L'adresse email est requise");
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    void shouldResetPasswordWithValidToken() {
        when(resetTokenRepository.findByTokenAndUsedFalse(anyString())).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode(anyString())).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        PasswordResetResponseDto response = passwordResetService.resetPassword(resetDto);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("réinitialisé avec succès");
        verify(resetTokenRepository).findByTokenAndUsedFalse("valid-token");
        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(user);
        verify(tokenRepository).deleteByUserId(user.getId());
        assertThat(resetToken.isUsed()).isTrue();
        verify(resetTokenRepository).save(resetToken);
    }

    @Test
    void shouldReturnErrorWhenPasswordsDoNotMatch() {
        resetDto.setConfirmPassword("differentPassword");

        PasswordResetResponseDto response = passwordResetService.resetPassword(resetDto);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Les mots de passe ne correspondent pas");
        verify(resetTokenRepository, never()).findByTokenAndUsedFalse(anyString());
    }

    @Test
    void shouldReturnErrorWhenPasswordTooShort() {
        resetDto.setNewPassword("1234567");

        PasswordResetResponseDto response = passwordResetService.resetPassword(resetDto);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("au moins 8 caractères");
        verify(resetTokenRepository, never()).findByTokenAndUsedFalse(anyString());
    }

    @Test
    void shouldReturnErrorWhenTokenIsInvalid() {
        when(resetTokenRepository.findByTokenAndUsedFalse(anyString())).thenReturn(Optional.empty());

        PasswordResetResponseDto response = passwordResetService.resetPassword(resetDto);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Lien de réinitialisation invalide");
        verify(resetTokenRepository).findByTokenAndUsedFalse("valid-token");
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldReturnErrorWhenTokenExpired() {
        resetToken.setExpiryDate(LocalDateTime.now().minusHours(1));
        when(resetTokenRepository.findByTokenAndUsedFalse(anyString())).thenReturn(Optional.of(resetToken));

        PasswordResetResponseDto response = passwordResetService.resetPassword(resetDto);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("a expiré");
        verify(resetTokenRepository).delete(resetToken);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldValidateTokenSuccessfully() {
        when(resetTokenRepository.findByTokenAndUsedFalse(anyString())).thenReturn(Optional.of(resetToken));

        PasswordResetResponseDto response = passwordResetService.validateResetToken("valid-token");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("Token valide");
    }

    @Test
    void shouldReturnInvalidWhenTokenNotFound() {
        when(resetTokenRepository.findByTokenAndUsedFalse(anyString())).thenReturn(Optional.empty());

        PasswordResetResponseDto response = passwordResetService.validateResetToken("invalid-token");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Token invalide");
    }
}