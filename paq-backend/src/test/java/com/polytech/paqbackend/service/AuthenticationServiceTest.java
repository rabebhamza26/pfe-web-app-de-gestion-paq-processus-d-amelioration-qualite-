package com.polytech.paqbackend.service;


import com.polytech.paqbackend.auth.AuthenticationRequest;
import com.polytech.paqbackend.auth.AuthenticationResponse;
import com.polytech.paqbackend.auth.AuthenticationService;
import com.polytech.paqbackend.auth.RegisterRequest;
import com.polytech.paqbackend.config.JwtService;
import com.polytech.paqbackend.entity.Role;
import com.polytech.paqbackend.entity.User;
import com.polytech.paqbackend.repository.UserRepository;
import com.polytech.paqbackend.token.Token;
import com.polytech.paqbackend.token.TokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TokenRepository tokenRepository;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationService authenticationService;

    private RegisterRequest registerRequest;
    private AuthenticationRequest authenticationRequest;
    private User user;
    private String encodedPassword;
    private String jwtToken;
    private String refreshToken;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setNomUtilisateur("Test User");
        registerRequest.setLogin("testLogin");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setRole(Role.SL);

        authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setLogin("testLogin");
        authenticationRequest.setPassword("password123");

        encodedPassword = "encodedPassword";
        jwtToken = "jwtToken";
        refreshToken = "refreshToken";

        user = User.builder()
                .id(1L)
                .nomUtilisateur("Test User")
                .login("testLogin")
                .email("test@example.com")
                .password(encodedPassword)
                .role(Role.SL)
                .active(true)
                .build();
    }

    // ─── REGISTER TESTS ─────────────────────────────────────────────────────────

    @Test
    void shouldRegisterUserSuccessfully() {
        // Given
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn(jwtToken);
        when(jwtService.generateRefreshToken(user)).thenReturn(refreshToken);
        when(tokenRepository.save(any(Token.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AuthenticationResponse response = authenticationService.register(registerRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo(jwtToken);
        assertThat(response.getRefreshToken()).isEqualTo(refreshToken);

        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateToken(user);
        verify(jwtService).generateRefreshToken(user);
        verify(tokenRepository).save(any(Token.class));
    }

    @Test
    void shouldThrowExceptionWhenUserRepositoryFailsOnRegister() {
        // Given
        when(passwordEncoder.encode(anyString())).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> authenticationService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error");

        verify(userRepository).save(any(User.class));
        verify(tokenRepository, never()).save(any(Token.class));
    }

    // ─── AUTHENTICATE TESTS ────────────────────────────────────────────────────

    @Test
    void shouldAuthenticateUserSuccessfully() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null); // authenticate() returns Authentication, but we don't need it
        when(userRepository.findByLogin(authenticationRequest.getLogin())).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn(jwtToken);
        when(jwtService.generateRefreshToken(user)).thenReturn(refreshToken);

        // Mock token revocation and save
        List<Token> validTokens = List.of(
                Token.builder().token("oldToken").expired(false).revoked(false).build()
        );
        when(tokenRepository.findAllValidTokenByUser(user.getId())).thenReturn(validTokens);
        when(tokenRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenRepository.save(any(Token.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AuthenticationResponse response = authenticationService.authenticate(authenticationRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo(jwtToken);
        assertThat(response.getRefreshToken()).isEqualTo(refreshToken);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByLogin("testLogin");
        verify(jwtService).generateToken(user);
        verify(jwtService).generateRefreshToken(user);

        // Vérifier que les anciens tokens sont révoqués
        validTokens.forEach(token -> {
            assertThat(token.isExpired()).isTrue();
            assertThat(token.isRevoked()).isTrue();
        });
        verify(tokenRepository).saveAll(validTokens);
        verify(tokenRepository).save(any(Token.class));
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundOnAuthenticate() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByLogin(authenticationRequest.getLogin())).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> authenticationService.authenticate(authenticationRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Utilisateur non trouvé");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByLogin("testLogin");
        verify(jwtService, never()).generateToken(any());
        verify(tokenRepository, never()).save(any(Token.class));
    }

    @Test
    void shouldThrowExceptionWhenAuthenticationFails() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Bad credentials"));

        // When & Then
        assertThatThrownBy(() -> authenticationService.authenticate(authenticationRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Bad credentials");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByLogin(anyString());
        verify(jwtService, never()).generateToken(any());
    }
}