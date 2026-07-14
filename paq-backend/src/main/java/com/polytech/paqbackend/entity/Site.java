package com.polytech.paqbackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;


import jakarta.persistence.*;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "site")
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Plant> plants;

    @ManyToMany(mappedBy = "sites", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<User> users;

    public Site() {}

    public Site(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public List<Plant> getPlants() { return plants; }
    public Set<User> getUsers() { return users; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPlants(List<Plant> plants) { this.plants = plants; }
    public void setUsers(Set<User> users) { this.users = users; }


}