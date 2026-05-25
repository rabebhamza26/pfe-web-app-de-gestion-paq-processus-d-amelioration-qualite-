package com.polytech.paqbackend.controller;

import com.polytech.paqbackend.dto.CreateUserRequest;
import com.polytech.paqbackend.dto.SiteUserDistributionDTO;
import com.polytech.paqbackend.dto.UpdateUserRequest;
import com.polytech.paqbackend.dto.UserResponseDto;
import com.polytech.paqbackend.entity.User;
import com.polytech.paqbackend.repository.UserRepository;
import com.polytech.paqbackend.service.EmailService;
import com.polytech.paqbackend.service.UserService;
import com.polytech.paqbackend.token.TokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TokenRepository tokenRepository;

    @Autowired
    public UserController(UserRepository userRepository, UserService userService,
                          PasswordEncoder passwordEncoder, EmailService emailService,
                          TokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.tokenRepository = tokenRepository;
    }


    @GetMapping("/sl/emails")
    @PreAuthorize("isAuthenticated()")

    public ResponseEntity<List<String>> getSlEmails() {
        List<User> users = userRepository.findAll();
        List<String> emails = users.stream()
                .filter(user -> user.getRole() != null &&
                        (user.getRole().name().equals("SL") ||
                                user.getRole().name().equals("ADMIN") ||
                                user.getRole().name().equals("RH")))
                .map(User::getEmail)
                .filter(email -> email != null && !email.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
        return ResponseEntity.ok(emails);
    }

    @GetMapping("/by-site")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SiteUserDistributionDTO>> getUsersBySite() {
        return ResponseEntity.ok(userService.getUsersDistributionBySite());
    }

    @GetMapping("/basic")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserResponseDto>> getAllUsersBasic() {
        List<User> users = userRepository.findAll();
        List<UserResponseDto> result = users.stream()
                .map(u -> UserResponseDto.builder()
                        .id(u.getId())
                        .nomUtilisateur(u.getNomUtilisateur())
                        .login(u.getLogin())
                        .email(u.getEmail())
                        .role(u.getRole())
                        .active(u.isActive())
                        .createdAt(u.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/all-emails")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> getAllEmailsAlternative() {
        return ResponseEntity.ok(userService.getAllEmails());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> createUser(@RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                        @RequestBody UpdateUserRequest request) {
        try {
            UserResponseDto updated = userService.updateUser(id, request);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleActive(id));
    }

    // ── méthode utilitaire : révoquer tous les tokens valides ──────────────
    private void revokeAllUserTokens(User user) {
        var validTokens = tokenRepository.findAllValidTokenByUser(user.getId());
        if (!validTokens.isEmpty()) {
            validTokens.forEach(t -> {
                t.setExpired(true);
                t.setRevoked(true);
            });
            tokenRepository.saveAll(validTokens);
        }
    }

    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resetUserPassword(@PathVariable Long id,
                                               @RequestBody Map<String, String> payload) {
        try {
            String newPassword = payload.get("newPassword");
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Veuillez fournir un nouveau mot de passe"));
            }

            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // 1. Révoquer tous les tokens existants
            revokeAllUserTokens(user);

            // 2. Encoder et sauvegarder le nouveau mot de passe
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            // 3. Envoyer l'email
            emailService.sendNewPasswordToUser(user.getEmail(), user.getLogin(), newPassword);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Mot de passe réinitialisé avec succès.",
                    "newPassword", newPassword));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Erreur: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/reset-password-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resetPasswordByAdmin(@PathVariable Long id,
                                                  @RequestBody Map<String, String> payload) {
        try {
            String newPassword = payload.get("newPassword");
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Veuillez fournir un nouveau mot de passe"));
            }

            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // 1. Révoquer tous les tokens existants
            revokeAllUserTokens(user);

            // 2. Encoder et sauvegarder le nouveau mot de passe
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            // 3. Envoyer l'email
            emailService.sendNewPasswordToUser(user.getEmail(), user.getLogin(), newPassword);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Mot de passe réinitialisé avec succès.",
                    "newPassword", newPassword));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Erreur: " + e.getMessage()));
        }
    }

    @PostMapping("/auth/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            String login = payload.get("login");

            User user = null;

            // findByEmail retourne User, pas Optional
            if (email != null && !email.trim().isEmpty()) {
                user = userRepository.findByEmail(email);
            }
            if (user == null && login != null && !login.trim().isEmpty()) {
                user = userRepository.findByLogin(login);
            }

            if (user == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Aucun utilisateur trouvé avec cet email ou login."
                ));
            }

            // Vérifier que l'utilisateur a un email valide
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Cet utilisateur n'a pas d'email enregistré. Veuillez contacter l'administrateur."
                ));
            }

            // Générer un mot de passe temporaire avec UUID
            String tempPassword = UUID.randomUUID().toString().substring(0, 8);
            user.setPassword(passwordEncoder.encode(tempPassword));
            userRepository.save(user);

            // Révoquer tous les tokens existants
            revokeAllUserTokens(user);

            // Envoyer par email
            emailService.sendNewPasswordToUser(user.getEmail(), user.getLogin(), tempPassword);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Un nouveau mot de passe a été envoyé à votre adresse email.\nVérifiez votre boîte de réception (et vos spams)."
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Erreur lors de la réinitialisation: " + e.getMessage()
            ));
        }
    }

    // Dans UserController.java, ajoutez cet endpoint pour tester l'authentification
    @GetMapping("/test-auth")
    public ResponseEntity<?> testAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(Map.of(
                "authenticated", auth != null && auth.isAuthenticated(),
                "name", auth != null ? auth.getName() : "null",
                "authorities", auth != null ? auth.getAuthorities().toString() : "null"
        ));
    }

    @GetMapping("/emails")
    public ResponseEntity<List<String>> getAllEmails() {
        return ResponseEntity.ok(userService.getAllEmails());
    }

    @GetMapping("/emails/active")
    public ResponseEntity<List<String>> getAllActiveUserEmails() {
        return ResponseEntity.ok(userService.getAllActiveUserEmails());
    }

    // ⭐ NOUVEAU: Récupérer les emails par Site ET Plant
    @GetMapping("/emails/by-site-plant")
    public ResponseEntity<List<String>> getEmailsBySiteAndPlant(
            @RequestParam Long siteId,
            @RequestParam Long plantId) {
        return ResponseEntity.ok(userService.getEmailsBySiteAndPlant(siteId, plantId));
    }

    // Récupérer les emails par Site uniquement
    @GetMapping("/emails/by-site")
    public ResponseEntity<List<String>> getEmailsBySite(@RequestParam Long siteId) {
        return ResponseEntity.ok(userService.getEmailsBySite(siteId));
    }

    // Récupérer les emails par Plant uniquement
    @GetMapping("/emails/by-plant")
    public ResponseEntity<List<String>> getEmailsByPlant(@RequestParam Long plantId) {
        return ResponseEntity.ok(userService.getEmailsByPlant(plantId));
    }

    // Récupérer les emails par plusieurs Sites et Plants
    @PostMapping("/emails/by-sites-plants")
    public ResponseEntity<List<String>> getEmailsBySitesAndPlants(
            @RequestBody Map<String, List<Long>> request) {
        List<Long> siteIds = request.get("siteIds");
        List<Long> plantIds = request.get("plantIds");
        return ResponseEntity.ok(userService.getEmailsBySitesAndPlants(siteIds, plantIds));
    }

    // Dans UserController.java - Ajoutez cet endpoint

    @GetMapping("/qm-emails/by-perimeter")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> getQMEmailsByPerimeter(Authentication authentication) {
        try {
            // Récupérer l'utilisateur connecté
            String email = authentication.getName();
            User currentUser = userRepository.findByEmailOrLoginWithPerimeter(email);

            if (currentUser == null) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            List<String> qmEmails = userService.getQMEmailsByPerimeter(currentUser);
            return ResponseEntity.ok(qmEmails);
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    // Endpoint existant (garder pour compatibilité)
    @GetMapping("/qm-emails")
    public ResponseEntity<List<String>> getQMEmails() {
        return ResponseEntity.ok(userService.getQMEmails());
    }

    @GetMapping("/sgl-emails/by-perimeter")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> getSGLEmailsByPerimeter(Authentication authentication) {
        try {
            String email = authentication.getName();
            User currentUser = userRepository.findByEmailOrLoginWithPerimeter(email);

            if (currentUser == null) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            List<String> sglEmails = userService.getSGLEmailsByPerimeter(currentUser);
            return ResponseEntity.ok(sglEmails);
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    // Dans UserController.java - Ajoutez ces endpoints

    @GetMapping("/hp-emails/by-perimeter")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> getHPEmailsByPerimeter(Authentication authentication) {
        try {
            String email = authentication.getName();
            User currentUser = userRepository.findByEmailOrLoginWithPerimeter(email);

            if (currentUser == null) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            List<String> hpEmails = userService.getHPEmailsByPerimeter(currentUser);
            return ResponseEntity.ok(hpEmails);
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/qmplant-emails/by-perimeter")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> getQMPlantEmailsByPerimeter(Authentication authentication) {
        try {
            String email = authentication.getName();
            User currentUser = userRepository.findByEmailOrLoginWithPerimeter(email);

            if (currentUser == null) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            List<String> qmPlantEmails = userService.getQMPlantEmailsByPerimeter(currentUser);
            return ResponseEntity.ok(qmPlantEmails);
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/hp-sgl-qmplant-emails/by-perimeter")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, List<String>>> getHPSGLQMPlantEmailsByPerimeter(Authentication authentication) {
        try {
            String email = authentication.getName();
            User currentUser = userRepository.findByEmailOrLoginWithPerimeter(email);

            if (currentUser == null) {
                Map<String, List<String>> emptyMap = Map.of(
                        "hp", new ArrayList<>(),
                        "sgl", new ArrayList<>(),
                        "qmPlant", new ArrayList<>()
                );
                return ResponseEntity.ok(emptyMap);
            }

            List<String> hpEmails = userService.getHPEmailsByPerimeter(currentUser);
            List<String> sglEmails = userService.getSGLEmailsByPerimeter(currentUser);
            List<String> qmPlantEmails = userService.getQMPlantEmailsByPerimeter(currentUser);

            Map<String, List<String>> result = Map.of(
                    "hp", hpEmails,
                    "sgl", sglEmails,
                    "qmPlant", qmPlantEmails
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, List<String>> emptyMap = Map.of(
                    "hp", new ArrayList<>(),
                    "sgl", new ArrayList<>(),
                    "qmPlant", new ArrayList<>()
            );
            return ResponseEntity.ok(emptyMap);
        }
    }
}