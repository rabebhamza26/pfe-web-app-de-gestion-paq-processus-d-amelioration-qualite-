package com.polytech.paqbackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

/**
 * Configuration Spring Security complète pour l'application PAQ LEONI.
 * CORRECTIF : /api/sites et /api/plants sont PUBLICS (utilisés avant login pour la sélection du site).
 */

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {

    // Filtres et providers pour la gestion de l'authentification JWT.
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider  authenticationProvider;
    private final LogoutHandler           logoutHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Désactive CSRF pour les API REST consommées par un client séparé.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(request -> {
                    var cfg = new CorsConfiguration();
                    cfg.setAllowedOriginPatterns(List.of(
                            "http://localhost:*",
                            "http://127.0.0.1:*",
                            "https://*.onrender.com",
                            "https://*.netlify.app"
                    ));
                    cfg.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                    cfg.setAllowedHeaders(List.of("*"));
                    cfg.setAllowCredentials(true);
                    return cfg;
                }))

                .authorizeHttpRequests(req -> req

                        // ── OPTIONS (pré-vol CORS) : TOUJOURS autorisé ───────────────
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ════════════════════════════════════════════════════════════
                        // ENDPOINTS PUBLICS  (sans JWT)
                        // ════════════════════════════════════════════════════════════

                        // Auth publique
                        .requestMatchers("/api/auth/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/refresh-token").permitAll()
                        .requestMatchers("/api/auth/forgot-password").permitAll()

                        .requestMatchers("/api/auth/reset-password").permitAll()
                        .requestMatchers("/api/auth/validate-reset-token").permitAll()


                        // CRITIQUE : Sites & Plants doivent être publics car utilisés
                        // AVANT la connexion (page SiteSelection → PlantSelection → Login)
                        .requestMatchers(HttpMethod.GET, "/api/sites").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/sites/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/plants").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/plants/**").permitAll()


                        // WebSocket
                        .requestMatchers("/ws/**", "/ws").permitAll()

                        // EMAILS - Accessible aux utilisateurs authentifiés
                        // ════════════════════════════════════════════════════════════
                        .requestMatchers(HttpMethod.GET, "/api/users/emails").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/sl/emails").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/all-emails").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/basic").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/test-auth").authenticated()

                        // Emails publics (sans auth)
                        .requestMatchers(HttpMethod.GET, "/api/users/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/entretiens-positifs/public/**").permitAll()

                        // Users partiellement publics
                        .requestMatchers(HttpMethod.GET, "/api/users/basic").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/all-emails").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/sl/emails").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/public/**").permitAll()

                        // Entretiens positifs email public
                        .requestMatchers(HttpMethod.GET, "/api/entretiens-positifs/public/**").permitAll()

                        // Swagger
                        .requestMatchers(
                                "/v2/api-docs", "/v3/api-docs", "/v3/api-docs/**",
                                "/swagger-ui.html", "/swagger-ui/**",
                                "/swagger-resources/**", "/webjars/**", "/error"
                        ).permitAll()

                        // ════════════════════════════════════════════════════════════
                        // ADMIN SEULEMENT
                        // ════════════════════════════════════════════════════════════
                        .requestMatchers("/api/auth/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,    "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,    "/api/users/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,   "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH,  "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,   "/api/sites/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/sites/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/sites/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,   "/api/plants/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/plants/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/plants/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,   "/api/segments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/segments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/segments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/*/reset-password").hasRole("ADMIN")


                        // ════════════════════════════════════════════════════════════
                        // TOUT LE RESTE : authentifié (JWT valide)
                        // Les permissions fines sont dans les @PreAuthorize des controllers
                        // ════════════════════════════════════════════════════════════
                        .anyRequest().authenticated()
                )

                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .addLogoutHandler(logoutHandler)
                        .logoutSuccessHandler((req, res, auth) ->
                                SecurityContextHolder.clearContext())
                );

        return http.build();
    }
}