package com.polytech.paqbackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public class EntretienFinalDTO {
    private String decision;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateEntretien;
    private String typeFaute;
    private String commentaireRH;
    private String destinataireEmail;

    // Dans EntretienFinalRequestDTO.java (si vous en avez un)
    private Double casca;

    public Double getCasca() { return casca; }
    public void setCasca(Double casca) { this.casca = casca; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getDestinataireEmail() { return destinataireEmail; }
    public void setDestinataireEmail(String destinataireEmail) { this.destinataireEmail = destinataireEmail; }

    public LocalDate getDateEntretien() { return dateEntretien; }
    public void setDateEntretien(LocalDate dateEntretien) { this.dateEntretien = dateEntretien; }

    public String getTypeFaute() { return typeFaute; }
    public void setTypeFaute(String typeFaute) { this.typeFaute = typeFaute; }

    public String getCommentaireRH() { return commentaireRH; }
    public void setCommentaireRH(String commentaireRH) { this.commentaireRH = commentaireRH; }
}