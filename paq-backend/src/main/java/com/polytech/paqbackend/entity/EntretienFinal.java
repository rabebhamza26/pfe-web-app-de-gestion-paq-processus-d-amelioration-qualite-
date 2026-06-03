package com.polytech.paqbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "entretien_final")
public class EntretienFinal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String matricule;

    private String decision;
    private LocalDate dateEntretien;
    private String typeFaute;

    // NOUVEAU CHAMP - causePrincipale (selon diagramme)
    @Column(columnDefinition = "TEXT")
    private String causePrincipale;

    @Column(columnDefinition = "TEXT")
    private String commentaireRH;

    // RENOMMÉ : casca -> ksk
    @Column(name = "ksk", nullable = true)
    private Double ksk;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public LocalDate getDateEntretien() { return dateEntretien; }
    public void setDateEntretien(LocalDate dateEntretien) { this.dateEntretien = dateEntretien; }

    public String getTypeFaute() { return typeFaute; }
    public void setTypeFaute(String typeFaute) { this.typeFaute = typeFaute; }

    public String getCausePrincipale() { return causePrincipale; }
    public void setCausePrincipale(String causePrincipale) { this.causePrincipale = causePrincipale; }

    public String getCommentaireRH() { return commentaireRH; }
    public void setCommentaireRH(String commentaireRH) { this.commentaireRH = commentaireRH; }

    public Double getKsk() { return ksk; }
    public void setKsk(Double ksk) { this.ksk = ksk; }
}