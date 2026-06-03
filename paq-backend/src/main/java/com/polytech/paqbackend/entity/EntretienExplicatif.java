package com.polytech.paqbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "premier_entretien")
public class EntretienExplicatif {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String matricule;
    private String typeFaute;
    private LocalDate dateFaute;

    @Column(columnDefinition="TEXT")
    private String description;

    @Column(columnDefinition="TEXT")
    private String mesuresCorrectives;

    @Column(columnDefinition="TEXT")
    private String commentaire;

    @Column(nullable = false)
    private boolean defautGrave = false;

    // NOUVEAU CHAMP KSK (optionnel) - remplace casca
    @Column(name = "ksk", nullable = true)
    private Double ksk;

    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters et Setters
    public Double getKsk() { return ksk; }
    public void setKsk(Double ksk) { this.ksk = ksk; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }

    public String getTypeFaute() { return typeFaute; }
    public void setTypeFaute(String typeFaute) { this.typeFaute = typeFaute; }

    public LocalDate getDateFaute() { return dateFaute; }
    public void setDateFaute(LocalDate dateFaute) { this.dateFaute = dateFaute; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMesuresCorrectives() { return mesuresCorrectives; }
    public void setMesuresCorrectives(String mesuresCorrectives) { this.mesuresCorrectives = mesuresCorrectives; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public boolean isDefautGrave() { return defautGrave; }
    public void setDefautGrave(boolean defautGrave) { this.defautGrave = defautGrave; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}