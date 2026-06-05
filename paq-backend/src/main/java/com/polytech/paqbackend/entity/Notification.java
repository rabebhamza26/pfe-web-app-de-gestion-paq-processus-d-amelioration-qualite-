package com.polytech.paqbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String destinataireLogin;  // login du destinataire (clé de filtrage)

    @Column(nullable = false)
    private String destinataireEmail;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private String type;  // INFO, SUCCESS, WARNING, ERROR, DEFUT_GRAVE

    @Column(nullable = false)
    private boolean lu = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private String typeEntretien;  // EXPLICATIF, ACCORD, MESURE, DECISION, FINAL, POSITIF, ARCHIVE

    private String matriculeCollaborateur;  // matricule du collaborateur concerné

    @Column(columnDefinition = "TEXT")
    private String data;  // Données JSON additionnelles

    public Notification() {}

    public Notification(String destinataireLogin, String destinataireEmail, String titre, String message, String type) {
        this.destinataireLogin = destinataireLogin;
        this.destinataireEmail = destinataireEmail;
        this.titre = titre;
        this.message = message;
        this.type = type;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDestinataireLogin() { return destinataireLogin; }
    public void setDestinataireLogin(String destinataireLogin) { this.destinataireLogin = destinataireLogin; }

    public String getDestinataireEmail() { return destinataireEmail; }
    public void setDestinataireEmail(String destinataireEmail) { this.destinataireEmail = destinataireEmail; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isLu() { return lu; }
    public void setLu(boolean lu) { this.lu = lu; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getTypeEntretien() { return typeEntretien; }
    public void setTypeEntretien(String typeEntretien) { this.typeEntretien = typeEntretien; }

    public String getMatriculeCollaborateur() { return matriculeCollaborateur; }
    public void setMatriculeCollaborateur(String matriculeCollaborateur) { this.matriculeCollaborateur = matriculeCollaborateur; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
}