package com.polytech.paqbackend.dto;

import java.time.LocalDate;
import java.util.List;

public class EntretienMesureRequestDTO {
    private String typeFaute;
    private String causesPrincipales;
    private String convention;
    private String planAction;
    private LocalDate dateEntretien;
    private String destinataireEmail;
    private String message;
    private List<String> destinatairesEmails;
    private Double ksk; // RENOMMÉ

    public String getTypeFaute() { return typeFaute; }
    public void setTypeFaute(String typeFaute) { this.typeFaute = typeFaute; }

    public String getCausesPrincipales() { return causesPrincipales; }
    public void setCausesPrincipales(String causesPrincipales) { this.causesPrincipales = causesPrincipales; }

    public String getDestinataireEmail() { return destinataireEmail; }
    public void setDestinataireEmail(String destinataireEmail) { this.destinataireEmail = destinataireEmail; }

    public String getConvention() { return convention; }
    public void setConvention(String convention) { this.convention = convention; }

    public String getPlanAction() { return planAction; }
    public void setPlanAction(String planAction) { this.planAction = planAction; }

    public LocalDate getDateEntretien() { return dateEntretien; }
    public void setDateEntretien(LocalDate dateEntretien) { this.dateEntretien = dateEntretien; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<String> getDestinatairesEmails() { return destinatairesEmails; }
    public void setDestinatairesEmails(List<String> destinatairesEmails) { this.destinatairesEmails = destinatairesEmails; }

    public Double getKsk() { return ksk; }
    public void setKsk(Double ksk) { this.ksk = ksk; }
}