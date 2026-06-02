package com.polytech.paqbackend.auth;

import com.polytech.paqbackend.config.JwtService;
import com.polytech.paqbackend.entity.ErrorResponse;
import com.polytech.paqbackend.entity.LoginRequest;
import com.polytech.paqbackend.entity.LoginResponse;
import com.polytech.paqbackend.entity.User;
import com.polytech.paqbackend.repository.UserRepository;
import com.polytech.paqbackend.token.Token;
import com.polytech.paqbackend.token.TokenRepository;
import com.polytech.paqbackend.token.TokenType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        User user = userRepository.findByLogin(request.getLogin());

        if (user == null) {
            return ResponseEntity.status(401).body(
                    new ErrorResponse("INVALID_CREDENTIALS", "Identifiants incorrects")
            );
        }

        // ✅ Fallback: mot de passe en clair → comparer puis migrer vers BCrypt
        String storedPassword = user.getPassword();
        boolean passwordValid;

        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            // Mot de passe déjà BCrypt
            passwordValid = passwordEncoder.matches(request.getPassword(), storedPassword);
        } else {
            // Mot de passe en clair → comparaison directe + migration automatique
            passwordValid = storedPassword.equals(request.getPassword());
            if (passwordValid) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                userRepository.save(user);
            }
        }

        if (!passwordValid) {
            return ResponseEntity.status(401).body(
                    new ErrorResponse("INVALID_CREDENTIALS", "Identifiants incorrects")
            );
        }

        if (!user.isActive()) {
            return ResponseEntity.status(403).body(
                    new ErrorResponse("USER_DISABLED", "Compte désactivé")
            );
        }

        if (request.getSiteName() != null && !request.getSiteName().isBlank()) {
            boolean belongsToSite = user.getSites().stream()
                    .anyMatch(s -> s.getName().equalsIgnoreCase(request.getSiteName()));
            if (!belongsToSite) {
                return ResponseEntity.status(403).body(
                        new ErrorResponse("WRONG_SITE",
                                "Vous n'appartenez pas au site " + request.getSiteName())
                );
            }
        }

        if (request.getPlantName() != null && !request.getPlantName().isBlank()) {
            boolean belongsToPlant = user.getPlants().stream()
                    .anyMatch(p -> p.getName().equalsIgnoreCase(request.getPlantName()));
            if (!belongsToPlant) {
                return ResponseEntity.status(403).body(
                        new ErrorResponse("WRONG_PLANT",
                                "Vous n'appartenez pas au plant " + request.getPlantName())
                );
            }
        }

        revokeAllUserTokens(user);

        String accessToken  = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        saveUserToken(user, accessToken);

        LoginResponse response = new LoginResponse(
                user.getId(),
                user.getNomUtilisateur(),
                user.getRole().name(),
                request.getSiteName(),
                request.getPlantName(),
                accessToken,
                refreshToken
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(
                    new ErrorResponse("INVALID_TOKEN", "Token manquant"));
        }
        String refreshToken = authHeader.substring(7);
        String login;
        try {
            login = jwtService.extractUsername(refreshToken);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(
                    new ErrorResponse("INVALID_TOKEN", "Token invalide"));
        }
        User user = userRepository.findByLogin(login);
        if (user == null || !user.isActive()) {
            return ResponseEntity.status(401).body(
                    new ErrorResponse("USER_NOT_FOUND", "Utilisateur introuvable ou désactivé"));
        }
        if (!jwtService.isTokenValid(refreshToken, login)) {
            return ResponseEntity.status(401).body(
                    new ErrorResponse("TOKEN_EXPIRED", "Token expiré"));
        }
        revokeAllUserTokens(user);
        String newAccessToken = jwtService.generateToken(user);
        saveUserToken(user, newAccessToken);
        return ResponseEntity.ok(new AuthenticationResponse(newAccessToken, refreshToken));
    }

    private void saveUserToken(User user, String jwtToken) {
        Token token = Token.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user) {
        var validTokens = tokenRepository.findAllValidTokenByUser(user.getId());
        if (validTokens.isEmpty()) return;
        validTokens.forEach(t -> {
            t.setExpired(true);
            t.setRevoked(true);
        });
        tokenRepository.saveAll(validTokens);
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var user = userRepository.findByLogin(request.getLogin());
        if (user == null) throw new RuntimeException("Utilisateur non trouvé");

        // Fallback: si le mot de passe n'est pas encodé en BCrypt, on compare en clair
        // puis on le migre automatiquement
        String storedPassword = user.getPassword();
        if (!storedPassword.startsWith("$2a$") && !storedPassword.startsWith("$2b$")) {
            // Mot de passe en clair → comparer directement, puis encoder
            if (!storedPassword.equals(request.getPassword())) {
                throw new org.springframework.security.authentication.BadCredentialsException("Mauvais mot de passe");
            }
            // Migrer vers BCrypt
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            userRepository.save(user);
        } else {
            // Mot de passe BCrypt → utiliser AuthenticationManager normalement
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getLogin(),
                            request.getPassword()
                    )
            );
        }

        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        revokeAllUserTokens(user);
        saveUserToken(user, jwtToken);

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }
}