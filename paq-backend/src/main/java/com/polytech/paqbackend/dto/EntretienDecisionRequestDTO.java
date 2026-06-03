package com.polytech.paqbackend.dto;

import java.time.LocalDate;
import java.util.List;

public class EntretienDecisionRequestDTO {
    private String typeFaute;
    private LocalDate dateEntretien;
    private String causePrincipale; // NOUVEAU
    private String decision;
    private String justification;
    private String destinataireEmail;
    private List<String> destinatairesEmails;
    private String messageOptionnel;
    private Double ksk; // RENOMMÉ

    public Double getKsk() { return ksk; }
    public void setKsk(Double ksk) { this.ksk = ksk; }

    public String getTypeFaute() { return typeFaute; }
    public void setTypeFaute(String typeFaute) { this.typeFaute = typeFaute; }

    public LocalDate getDateEntretien() { return dateEntretien; }
    public void setDateEntretien(LocalDate dateEntretien) { this.dateEntretien = dateEntretien; }

    public String getCausePrincipale() { return causePrincipale; }
    public void setCausePrincipale(String causePrincipale) { this.causePrincipale = causePrincipale; }

    public String getDestinataireEmail() { return destinataireEmail; }
    public void setDestinataireEmail(String destinataireEmail) { this.destinataireEmail = destinataireEmail; }


    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }

    public List<String> getDestinatairesEmails() { return destinatairesEmails; }
    public void setDestinatairesEmails(List<String> destinatairesEmails) { this.destinatairesEmails = destinatairesEmails; }

    public String getMessageOptionnel() { return messageOptionnel; }
    public void setMessageOptionnel(String messageOptionnel) { this.messageOptionnel = messageOptionnel; }
}