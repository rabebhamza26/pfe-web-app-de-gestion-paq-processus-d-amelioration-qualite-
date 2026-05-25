package com.polytech.paqbackend.dto;

public class EntretiensTotalsDTO {
    private long explicatif;
    private long accord;
    private long mesure;
    private long decision;
    private long final_;
    private long total;

    public EntretiensTotalsDTO() {}

    public long getExplicatif() { return explicatif; }
    public void setExplicatif(long explicatif) { this.explicatif = explicatif; }

    public long getAccord() { return accord; }
    public void setAccord(long accord) { this.accord = accord; }

    public long getMesure() { return mesure; }
    public void setMesure(long mesure) { this.mesure = mesure; }

    public long getDecision() { return decision; }
    public void setDecision(long decision) { this.decision = decision; }

    public long getFinal() { return final_; }
    public void setFinal(long final_) { this.final_ = final_; }

    public long getTotal() {
        return explicatif + accord + mesure + decision + final_;
    }
    public void setTotal(long total) { this.total = total; }
}