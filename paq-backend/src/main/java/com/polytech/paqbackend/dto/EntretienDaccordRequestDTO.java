package com.polytech.paqbackend.dto;

import java.time.LocalDate;

public class EntretienDaccordRequestDTO {
    private LocalDate date;
    private String mesuresProposees;
    private String destinataireEmail;
    private String typeFaute;
    private String causeFaute;
    private Double ksk; // RENOMMÉ

    public Double getKsk() { return ksk; }
    public void setKsk(Double ksk) { this.ksk = ksk; }

    public String getDestinataireEmail() { return destinataireEmail; }
    public void setDestinataireEmail(String destinataireEmail) { this.destinataireEmail = destinataireEmail; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getMesuresProposees() { return mesuresProposees; }
    public void setMesuresProposees(String mesuresProposees) { this.mesuresProposees = mesuresProposees; }

    public String getTypeFaute() { return typeFaute; }
    public void setTypeFaute(String typeFaute) { this.typeFaute = typeFaute; }

    public String getCauseFaute() { return causeFaute; }
    public void setCauseFaute(String causeFaute) { this.causeFaute = causeFaute; }
}