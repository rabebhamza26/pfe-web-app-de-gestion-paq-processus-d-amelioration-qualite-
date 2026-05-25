package com.polytech.paqbackend.dto;

import java.util.List;

public class ValiderEntretienPositifRequest {

    private List<String> matricules;
    private String slDestinataire;
    private String dateEnvoi;
    private String note;

    public List<String> getMatricules() { return matricules; }
    public void setMatricules(List<String> matricules) { this.matricules = matricules; }

    public String getSlDestinataire() { return slDestinataire; }
    public void setSlDestinataire(String slDestinataire) { this.slDestinataire = slDestinataire; }

    public String getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(String dateEnvoi) { this.dateEnvoi = dateEnvoi; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}