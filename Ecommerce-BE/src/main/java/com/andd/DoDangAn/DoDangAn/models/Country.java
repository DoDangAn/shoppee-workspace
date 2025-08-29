package com.andd.DoDangAn.DoDangAn.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "countries")
@Data
public class Country {


    @Id
    @Size(max = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "countryId")
    private String countryId;

    @Column(name = "countryName")
    private String countryName;

}