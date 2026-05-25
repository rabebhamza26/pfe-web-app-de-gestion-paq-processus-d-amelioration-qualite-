package com.polytech.paqbackend.dto;



public class EntretienEvolutionDTO {
    private String periode;
    private long count;

    public EntretienEvolutionDTO() {}

    public EntretienEvolutionDTO(String periode, long count) {
        this.periode = periode;
        this.count = count;
    }

    public String getPeriode() { return periode; }
    public void setPeriode(String periode) { this.periode = periode; }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}