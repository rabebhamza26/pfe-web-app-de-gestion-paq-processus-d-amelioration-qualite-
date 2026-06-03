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
    private boolean hasPaq;
    private Boolean hasActivePaq;
    private Boolean peutCreerPaq;
    private LocalDate prochainPaqDate;

    // Constructeur pour les requêtes existantes (9 paramètres)
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
        this.hasActivePaq = false;
        this.peutCreerPaq = false;
        this.prochainPaqDate = null;
    }

    // Constructeur complet pour les nouvelles requêtes (12 paramètres)
    public CollaborateurDTO(String matricule, String nom, String prenom, LocalDate hireDate,
                            String segment, Integer niveau, String status, Boolean actif,
                            Boolean archived, Boolean depart, Boolean hasActivePaq,
                            Boolean peutCreerPaq, LocalDate prochainPaqDate) {
        this.matricule = matricule;
        this.nom = nom;
        this.prenom = prenom;
        this.hireDate = hireDate;
        this.segment = segment;
        this.niveau = niveau != null ? niveau : 0;
        this.statut = status;
        this.hasPaq = hasActivePaq != null && hasActivePaq;
        this.hasActivePaq = hasActivePaq;
        this.peutCreerPaq = peutCreerPaq;
        this.prochainPaqDate = prochainPaqDate;
        this.derniereFaute = null;
    }

    // Constructeur alternatif pour getCollaboratorsBySegmentsWithDate
    public CollaborateurDTO(String matricule, String nom, String prenom,
                            String segment, LocalDate hireDate,
                            int niveau, LocalDate derniereFaute,
                            String statut, boolean hasPaq,
                            Boolean hasActivePaq, Boolean peutCreerPaq, LocalDate prochainPaqDate) {
        this.matricule = matricule;
        this.nom = nom;
        this.prenom = prenom;
        this.segment = segment;
        this.hireDate = hireDate;
        this.niveau = niveau;
        this.derniereFaute = derniereFaute;
        this.statut = statut;
        this.hasPaq = hasPaq;
        this.hasActivePaq = hasActivePaq;
        this.peutCreerPaq = peutCreerPaq;
        this.prochainPaqDate = prochainPaqDate;
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

    public Boolean getHasActivePaq() { return hasActivePaq; }
    public void setHasActivePaq(Boolean hasActivePaq) { this.hasActivePaq = hasActivePaq; }

    public Boolean getPeutCreerPaq() { return peutCreerPaq; }
    public void setPeutCreerPaq(Boolean peutCreerPaq) { this.peutCreerPaq = peutCreerPaq; }

    public LocalDate getProchainPaqDate() { return prochainPaqDate; }
    public void setProchainPaqDate(LocalDate prochainPaqDate) { this.prochainPaqDate = prochainPaqDate; }
}