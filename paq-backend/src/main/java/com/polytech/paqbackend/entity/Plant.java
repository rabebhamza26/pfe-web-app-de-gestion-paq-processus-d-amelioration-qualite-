package com.polytech.paqbackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jdk.jshell.Snippet;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plant")
public class Plant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    @JsonIgnore
    private Site site;


    @OneToMany(mappedBy = "plant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Segment> segments = new ArrayList<>();

    public Plant() {}


    public Plant(String name, Site site) {
        this.name = name;
        this.site = site;
    }



    public Long getId() { return id; }
    public String getName() { return name; }
    public Site getSite() { return site; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setSite(Site site) { this.site = site; }

    public List<Segment> getSegments() {
        return segments;
    }

    public void setSegments(List<Segment> segments) {
        this.segments = segments;
    }
}