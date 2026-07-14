package com.polytech.paqbackend;

// Import de l'annotation JUnit 5 pour définir une méthode de test.
import org.junit.jupiter.api.Test;
// Import de Spring Boot pour charger le contexte d'application complet.
import org.springframework.boot.test.context.SpringBootTest;
// Import de l'annotation pour activer le profil Spring "test".
import org.springframework.test.context.ActiveProfiles;

// Indique à Spring Boot de démarrer le contexte d'application pour ce test.
@SpringBootTest

// Utilise le profil "test" pour charger des configurations spécifiques aux tests.
@ActiveProfiles("test")
class PaqBackendApplicationTests {

    // Méthode de test exécutée par JUnit.
    @Test
    void contextLoads() {
        // Le test réussit lorsque le contexte Spring Boot se charge sans erreur.
    }
}