// com/polytech/paqbackend/service/PasswordResetService.java
package com.polytech.paqbackend.service;

import com.polytech.paqbackend.dto.PasswordResetDto;
import com.polytech.paqbackend.dto.PasswordResetRequestDto;
import com.polytech.paqbackend.dto.PasswordResetResponseDto;
import com.polytech.paqbackend.entity.PasswordResetToken;
import com.polytech.paqbackend.entity.User;
import com.polytech.paqbackend.repository.PasswordResetTokenRepository;
import com.polytech.paqbackend.repository.UserRepository;
import com.polytech.paqbackend.token.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    // Service responsable de la réinitialisation de mot de passe et de l'envoi d'emails.
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;

    @Value("${spring.mail.username:paqleoni@gmail.com}")
    private String fromEmail;

    @PostConstruct
    public void init() {
        log.info("PasswordResetService initialisé avec baseUrl: {}, fromEmail: {}", baseUrl, fromEmail);
    }

    /**
     * Envoie un email de réinitialisation de mot de passe
     */
    @Transactional
    public PasswordResetResponseDto sendPasswordResetEmail(PasswordResetRequestDto request) {
        log.info("=== DÉBUT sendPasswordResetEmail ===");
        log.info("Email reçu: {}", request.getEmail());

        // Validation de l'email
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            log.warn("Email vide ou null");
            return PasswordResetResponseDto.builder()
                    .success(false)
                    .message("L'adresse email est requise")
                    .build();
        }

        // Recherche de l'utilisateur
        User user = userRepository.findByEmailIgnoreCase(request.getEmail()).orElse(null);
        log.info("Utilisateur trouvé: {}", user != null ? user.getEmail() : "NON TROUVÉ");

        // Sécurité: on ne révèle pas si l'email existe ou non
        if (user == null) {
            log.warn("Tentative de réinitialisation pour email inexistant: {}", request.getEmail());
            return PasswordResetResponseDto.builder()
                    .success(true)
                    .message("Si un compte existe avec cet email, un lien de réinitialisation vous sera envoyé.")
                    .build();
        }

        // Vérifier que l'utilisateur est actif
        if (!user.isActive()) {
            log.warn("Tentative de réinitialisation pour compte inactif: {}", user.getEmail());
            return PasswordResetResponseDto.builder()
                    .success(true)
                    .message("Si un compte existe avec cet email, un lien de réinitialisation vous sera envoyé.")
                    .build();
        }

        try {
            // Supprimer les anciens tokens
            resetTokenRepository.deleteByUserId(user.getId());

            // Créer un nouveau token
            String token = UUID.randomUUID().toString();
            log.info("Token généré: {}", token);

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusHours(24))
                    .used(false)
                    .build();
            resetTokenRepository.save(resetToken);

            // Construire l'URL de réinitialisation
            String resetUrl = baseUrl + "/reset-password?token=" + token;
            log.info("URL de réinitialisation: {}", resetUrl);

            // Envoyer l'email
            boolean emailSent = sendResetEmailDirect(user, resetUrl);
            log.info("Email envoyé avec succès: {}", emailSent);

            if (emailSent) {
                return PasswordResetResponseDto.builder()
                        .success(true)
                        .message("Un email de réinitialisation a été envoyé à votre adresse email.")
                        .build();
            } else {
                return PasswordResetResponseDto.builder()
                        .success(false)
                        .message("Une erreur est survenue lors de l'envoi de l'email. Veuillez réessayer.")
                        .build();
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de réinitialisation", e);
            return PasswordResetResponseDto.builder()
                    .success(false)
                    .message("Une erreur technique est survenue. Veuillez réessayer plus tard.")
                    .build();
        }
    }

    /**
     * Envoie l'email de réinitialisation directement
     */
    private boolean sendResetEmailDirect(User user, String resetUrl) {
        try {
            String userName = user.getNomUtilisateur() != null ? user.getNomUtilisateur() : user.getLogin();

            String emailContent = String.format(
                    "Bonjour %s,\n\n" +
                            "Vous avez demandé la réinitialisation de votre mot de passe pour le système PAQ.\n\n" +
                            "Cliquez sur le lien ci-dessous pour réinitialiser votre mot de passe :\n" +
                            "%s\n\n" +
                            "Ce lien est valable 24 heures.\n\n" +
                            "Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.\n" +
                            "Votre mot de passe actuel reste valide.\n\n" +
                            "Pour des raisons de sécurité, ne partagez jamais ce lien.\n\n" +
                            "Cordialement,\n" +
                            "L'équipe PAQ System\n\n" +
                            "---\n" +
                            "Ceci est un email automatique, merci de ne pas y répondre.",
                    userName, resetUrl);

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(user.getEmail());
            mailMessage.setFrom(fromEmail);
            mailMessage.setSubject("PAQ System - Réinitialisation de votre mot de passe");
            mailMessage.setText(emailContent);

            log.info("Tentative d'envoi d'email à: {}", user.getEmail());
            log.info("De: {}", fromEmail);
            log.info("Sujet: {}", mailMessage.getSubject());

            mailSender.send(mailMessage);

            log.info("✅ Email envoyé avec succès à: {}", user.getEmail());
            return true;

        } catch (Exception e) {
            log.error("❌ Erreur détaillée d'envoi d'email: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Envoie un email (méthode générique)
     */
    private boolean sendEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(to);
            mailMessage.setFrom(fromEmail);
            mailMessage.setSubject(subject);
            mailMessage.setText(content);
            mailSender.send(mailMessage);
            log.info("Email envoyé avec succès à: {}", to);
            return true;
        } catch (Exception e) {
            log.error("Erreur d'envoi d'email à {}: {}", to, e.getMessage(), e);
            return false;
        }
    }

    // com/polytech/paqbackend/service/PasswordResetService.java

    /**
     * Réinitialise le mot de passe avec un token valide
     */
    @Transactional
    public PasswordResetResponseDto resetPassword(PasswordResetDto request) {
        // Validation des mots de passe
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return PasswordResetResponseDto.builder()
                    .success(false)
                    .message("Les mots de passe ne correspondent pas")
                    .build();
        }

        // ⭐ NOUVELLE VALIDATION : seulement 8 caractères minimum
        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            return PasswordResetResponseDto.builder()
                    .success(false)
                    .message("Le mot de passe doit contenir au moins 8 caractères")
                    .build();
        }

        try {
            // Recherche du token
            PasswordResetToken resetToken = resetTokenRepository.findByTokenAndUsedFalse(request.getToken())
                    .orElse(null);

            if (resetToken == null) {
                log.warn("Token de réinitialisation invalide: {}", request.getToken());
                return PasswordResetResponseDto.builder()
                        .success(false)
                        .message("Lien de réinitialisation invalide. Veuillez faire une nouvelle demande.")
                        .build();
            }

            // Vérifier l'expiration
            if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                log.warn("Token de réinitialisation expiré: {}", request.getToken());
                resetTokenRepository.delete(resetToken);
                return PasswordResetResponseDto.builder()
                        .success(false)
                        .message("Ce lien de réinitialisation a expiré. Veuillez faire une nouvelle demande.")
                        .build();
            }

            // Récupérer l'utilisateur
            User user = resetToken.getUser();

            // Vérifier que le nouveau mot de passe est différent de l'ancien
            if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
                return PasswordResetResponseDto.builder()
                        .success(false)
                        .message("Le nouveau mot de passe doit être différent de l'ancien")
                        .build();
            }

            // Mettre à jour le mot de passe
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            // Marquer le token comme utilisé
            resetToken.setUsed(true);
            resetTokenRepository.save(resetToken);

            // Révoquer tous les tokens JWT existants de l'utilisateur
            tokenRepository.deleteByUserId(user.getId());

            log.info("Mot de passe réinitialisé avec succès pour l'utilisateur: {}", user.getLogin());

            return PasswordResetResponseDto.builder()
                    .success(true)
                    .message("Votre mot de passe a été réinitialisé avec succès. Veuillez vous connecter avec votre nouveau mot de passe.")
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la réinitialisation du mot de passe", e);
            return PasswordResetResponseDto.builder()
                    .success(false)
                    .message("Une erreur technique est survenue. Veuillez réessayer plus tard.")
                    .build();
        }
    }



    /**
     * Vérifie si un token de réinitialisation est valide
     */
    public PasswordResetResponseDto validateResetToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return PasswordResetResponseDto.builder()
                    .success(false)
                    .message("Token invalide")
                    .build();
        }

        try {
            PasswordResetToken resetToken = resetTokenRepository.findByTokenAndUsedFalse(token)
                    .orElse(null);

            if (resetToken == null) {
                return PasswordResetResponseDto.builder()
                        .success(false)
                        .message("Token invalide")
                        .build();
            }

            if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                resetTokenRepository.delete(resetToken);
                return PasswordResetResponseDto.builder()
                        .success(false)
                        .message("Token expiré")
                        .build();
            }

            return PasswordResetResponseDto.builder()
                    .success(true)
                    .message("Token valide")
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la validation du token", e);
            return PasswordResetResponseDto.builder()
                    .success(false)
                    .message("Erreur de validation")
                    .build();
        }
    }

    /**
     * Vérifie la force du mot de passe
     */
    private boolean isPasswordStrong(String password) {
        return password != null && password.length() >= 8;
    }



    }
