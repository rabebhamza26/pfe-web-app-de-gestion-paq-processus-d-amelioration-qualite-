package com.polytech.paqbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PaqBackendApplication {

    // Point d'entrée principal de l'application Spring Boot.
    // Cette classe démarre le serveur et active la planification des tâches.
    public static void main(String[] args) {
        SpringApplication.run(PaqBackendApplication.class, args);
    }

}
