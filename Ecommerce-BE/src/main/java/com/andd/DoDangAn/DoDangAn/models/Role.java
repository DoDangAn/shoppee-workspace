package com.andd.DoDangAn.DoDangAn.models;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(name = "name")
    private String name;

    // Constructors
    public Role() {}

    public Role(String id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
