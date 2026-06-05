package com.polytech.paqbackend.dto;

import java.time.LocalDate;
import java.util.List;

public class EntretienDecisionRequestDTO {
    private String typeFaute;
    private LocalDate dateEntretien;
    private String synthese;
    private String decision;
    private String justification;
    private String destinataireEmail; // Gardé pour compatibilité
    private List<String> destinatairesEmails; // Liste d'emails pour SL
    private String messageOptionnel; // Message optionnel
    private Double casca; // NOUVEAU

    public Double getCasca() {
        return casca;
    }

    public void setCasca(Double casca) {
        this.casca = casca;
    }

    // GETTERS & SETTERS
    public String getTypeFaute() { return typeFaute; }
    public void setTypeFaute(String typeFaute) { this.typeFaute = typeFaute; }

    public LocalDate getDateEntretien() { return dateEntretien; }
    public void setDateEntretien(LocalDate dateEntretien) { this.dateEntretien = dateEntretien; }

    public String getDestinataireEmail() { return destinataireEmail; }
    public void setDestinataireEmail(String destinataireEmail) { this.destinataireEmail = destinataireEmail; }

    public String getSynthese() { return synthese; }
    public void setSynthese(String synthese) { this.synthese = synthese; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }

    public List<String> getDestinatairesEmails() { return destinatairesEmails; }
    public void setDestinatairesEmails(List<String> destinatairesEmails) { this.destinatairesEmails = destinatairesEmails; }

    public String getMessageOptionnel() { return messageOptionnel; }
    public void setMessageOptionnel(String messageOptionnel) { this.messageOptionnel = messageOptionnel; }
}