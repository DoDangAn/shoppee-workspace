package com.andd.DoDangAn.DoDangAn.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Set;

@Entity
@Table(name = "Categories")
@Data
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "categoryID")
    private String categoryID;

    @Column(name = "categoryName")
    private String categoryName;

    private String description;

    @ManyToMany(mappedBy = "categories")
    private Set<Product> products;

    @OneToMany(mappedBy = "category")
    private Set<Advertise> advertises;

    @OneToMany(mappedBy = "category")
    private Set<ProductVariant> productVariants;
}