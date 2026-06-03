package com.polytech.paqbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "entretien_decision")
public class EntretienDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String matricule;
    private String typeFaute;
    private LocalDate dateEntretien;

    // NOUVEAU CHAMP - causePrincipale (selon diagramme)
    @Column(columnDefinition = "TEXT")
    private String causePrincipale;

    @Column(columnDefinition = "TEXT")
    private String decision;

    @Column(columnDefinition = "TEXT")
    private String justification;

    private LocalDate dateCreation;

    // RENOMMÉ : casca -> ksk
    @Column(name = "ksk", nullable = true)
    private Double ksk;

    @Column(name = "valide_sl")
    private boolean valideSL = false;

    @Column(name = "valide_hp_sgl")
    private boolean valideHPSGL = false;

    @Column(name = "valide_qm_plant")
    private boolean valideQMPlant = false;

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }

    public String getTypeFaute() { return typeFaute; }
    public void setTypeFaute(String typeFaute) { this.typeFaute = typeFaute; }

    public LocalDate getDateEntretien() { return dateEntretien; }
    public void setDateEntretien(LocalDate dateEntretien) { this.dateEntretien = dateEntretien; }

    public String getCausePrincipale() { return causePrincipale; }
    public void setCausePrincipale(String causePrincipale) { this.causePrincipale = causePrincipale; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }

    public LocalDate getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDate dateCreation) { this.dateCreation = dateCreation; }

    public boolean isValideSL() { return valideSL; }
    public void setValideSL(boolean valideSL) { this.valideSL = valideSL; }

    public boolean isValideHPSGL() { return valideHPSGL; }
    public void setValideHPSGL(boolean valideHPSGL) { this.valideHPSGL = valideHPSGL; }

    public boolean isValideQMPlant() { return valideQMPlant; }
    public void setValideQMPlant(boolean valideQMPlant) { this.valideQMPlant = valideQMPlant; }

    public Double getKsk() { return ksk; }
    public void setKsk(Double ksk) { this.ksk = ksk; }
}