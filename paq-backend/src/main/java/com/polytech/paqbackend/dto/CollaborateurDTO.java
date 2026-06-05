package com.polytech.paqbackend.dto;

import java.time.LocalDate;

public class CollaborateurDTO {
    private String matricule;
    private String nom;
    private String prenom;
    private String segment;
    private LocalDate hireDate;
    private int niveau;
    private LocalDate derniereFaute;
    private String statut;
    private boolean hasPaq;  // Renommé de peutCreerPaq à hasPaq

    // Constructeur complet
    public CollaborateurDTO(String matricule, String nom, String prenom,
                            String segment, LocalDate hireDate,
                            int niveau, LocalDate derniereFaute,
                            String statut, boolean hasPaq) {
        this.matricule = matricule;
        this.nom = nom;
        this.prenom = prenom;
        this.segment = segment;
        this.hireDate = hireDate;
        this.niveau = niveau;
        this.derniereFaute = derniereFaute;
        this.statut = statut;
        this.hasPaq = hasPaq;
    }

    // Getters et Setters
    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getSegment() { return segment; }
    public void setSegment(String segment) { this.segment = segment; }

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }

    public int getNiveau() { return niveau; }
    public void setNiveau(int niveau) { this.niveau = niveau; }

    public LocalDate getDerniereFaute() { return derniereFaute; }
    public void setDerniereFaute(LocalDate derniereFaute) { this.derniereFaute = derniereFaute; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public boolean isHasPaq() { return hasPaq; }
    public void setHasPaq(boolean hasPaq) { this.hasPaq = hasPaq; }
}