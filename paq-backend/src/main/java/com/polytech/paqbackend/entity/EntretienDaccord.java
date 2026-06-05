package com.polytech.paqbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "entretien_daccord")
public class EntretienDaccord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "VARCHAR(100)")
    private String typeFaute;

    private String matricule;
    private LocalDate date;

    @Column(columnDefinition = "TEXT")
    private String causeFaute;

    @Column(columnDefinition = "TEXT")
    private String mesuresProposees;

    private LocalDateTime createdAt = LocalDateTime.now();

    // true = SL a soumis pour validation (email envoyé au QM)
    private Boolean valide = false;

    // true = QM_SEGMENT a validé définitivement (enregistré dans le dossier PAQ)
    private Boolean valideQM = false;

    // NOUVEAU CHAMP CASCA (optionnel)
    @Column(name = "casca", nullable = true)
    private Double casca;




    // Getters et Setters

    public Double getCasca() {
        return casca;
    }

    public void setCasca(Double casca) {
        this.casca = casca;
    }

    public Boolean getValide() { return valide; }
    public void setValide(Boolean valide) { this.valide = valide; }

    public Boolean getValideQM() { return valideQM; }
    public void setValideQM(Boolean valideQM) { this.valideQM = valideQM; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getTypeFaute() { return typeFaute; }
    public void setTypeFaute(String typeFaute) { this.typeFaute = typeFaute; }

    public String getCauseFaute() { return causeFaute; }
    public void setCauseFaute(String causeFaute) { this.causeFaute = causeFaute; }

    public String getMesuresProposees() { return mesuresProposees; }
    public void setMesuresProposees(String mesuresProposees) { this.mesuresProposees = mesuresProposees; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}