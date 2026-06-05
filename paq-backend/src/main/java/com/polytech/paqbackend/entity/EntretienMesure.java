package com.polytech.paqbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "entretien_mesure")
public class EntretienMesure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String matricule;
    private String typeFaute;

    @Column(columnDefinition = "TEXT")
    private String causesPrincipales;

    @Column(columnDefinition = "TEXT")
    private String convention;

    @Column(columnDefinition = "TEXT")
    private String planAction;

    private LocalDate dateEntretien;
    private LocalDate dateCreation;
    private boolean alerteEnvoyee = false;

    // NOUVEAU CHAMP CASCA (optionnel)
    @Column(name = "casca", nullable = true)
    private Double casca;

    // ⭐ Avec @Column pour correspondre aux noms des colonnes dans la base
    @Column(name = "valide_sl")
    private boolean valideSL = false;

    @Column(name = "valide_parqm")
    private boolean valideQM = false;

    @Column(name = "valide_sgl")
    private boolean valideSGL = false;

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }

    public String getTypeFaute() { return typeFaute; }
    public void setTypeFaute(String typeFaute) { this.typeFaute = typeFaute; }

    public String getCausesPrincipales() { return causesPrincipales; }
    public void setCausesPrincipales(String causesPrincipales) { this.causesPrincipales = causesPrincipales; }

    public String getConvention() { return convention; }
    public void setConvention(String convention) { this.convention = convention; }

    public String getPlanAction() { return planAction; }
    public void setPlanAction(String planAction) { this.planAction = planAction; }

    public LocalDate getDateEntretien() { return dateEntretien; }
    public void setDateEntretien(LocalDate dateEntretien) { this.dateEntretien = dateEntretien; }


    public LocalDate getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDate dateCreation) { this.dateCreation = dateCreation; }

    public boolean isAlerteEnvoyee() { return alerteEnvoyee; }
    public void setAlerteEnvoyee(boolean alerteEnvoyee) { this.alerteEnvoyee = alerteEnvoyee; }

    public boolean isValideSL() { return valideSL; }
    public void setValideSL(boolean valideSL) { this.valideSL = valideSL; }

    public boolean isValideQM() { return valideQM; }
    public void setValideQM(boolean valideQM) { this.valideQM = valideQM; }

    public boolean isValideSGL() { return valideSGL; }
    public void setValideSGL(boolean valideSGL) { this.valideSGL = valideSGL; }

    public Double getCasca() {
        return casca;
    }

    public void setCasca(Double casca) {
        this.casca = casca;
    }
}