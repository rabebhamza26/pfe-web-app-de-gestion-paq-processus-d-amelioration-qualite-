// com/polytech/paqbackend/controller/PasswordResetController.java
package com.polytech.paqbackend.controller;

import com.polytech.paqbackend.dto.PasswordResetDto;
import com.polytech.paqbackend.dto.PasswordResetRequestDto;
import com.polytech.paqbackend.dto.PasswordResetResponseDto;
import com.polytech.paqbackend.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    /**
     * Demande de réinitialisation de mot de passe
     * Envoie un email avec lien de réinitialisation
     *
     * @param request DTO contenant l'email de l'utilisateur
     * @return ResponseEntity avec le statut de la demande
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetResponseDto> forgotPassword(
            @Valid @RequestBody PasswordResetRequestDto request) {
        log.info("Requête de réinitialisation de mot de passe pour: {}", request.getEmail());
        PasswordResetResponseDto response = passwordResetService.sendPasswordResetEmail(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Réinitialisation du mot de passe avec token
     *
     * @param request DTO contenant le token et les nouveaux mots de passe
     * @return ResponseEntity avec le statut de la réinitialisation
     */
    @PostMapping("/reset-password")
    public ResponseEntity<PasswordResetResponseDto> resetPassword(
            @Valid @RequestBody PasswordResetDto request) {
        log.info("Tentative de réinitialisation de mot de passe");

        PasswordResetResponseDto response = passwordResetService.resetPassword(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            // Retourner 200 avec success=false, pas 400
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Validation du token de réinitialisation
     *
     * @param token Token de réinitialisation à valider
     * @return ResponseEntity indiquant si le token est valide
     */
    @GetMapping("/validate-reset-token")
    public ResponseEntity<PasswordResetResponseDto> validateResetToken(
            @RequestParam String token) {
        log.info("Validation du token de réinitialisation: {}",
                token.substring(0, Math.min(8, token.length())) + "...");

        PasswordResetResponseDto response = passwordResetService.validateResetToken(token);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}