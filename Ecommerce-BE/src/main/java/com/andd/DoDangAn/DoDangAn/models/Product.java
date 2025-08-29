package com.andd.DoDangAn.DoDangAn.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "products")
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "productID")
    private String id;

    @Column(name = "productName")
    @NotNull
    @NotBlank(message = "product name cannot be null")
    @Size(min = 3, max = 300)
    private String productName;

    @Column(name = "price")
    private Double price;

    @Column(name = "rate")
    private Integer rate;

    @NotNull
    @Size(min = 5, max = 1000)
    private String description;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "imageUrl")
    @NotNull(message = "Ảnh không được để trống")
    private String imageUrl;

    @Column(name = "videoPublicId")
    private String videoPublicId;

    @Column(name = "likes")
    private Boolean likes;

    @Column(name = "releaseDate")
    private LocalDateTime releaseDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "countryID", nullable = false)
    private Country country;

    @Column(name = "viewCount")
    private Integer viewCount;

    // Switched to LAZY to avoid loading categories for every product when only basic product info is needed
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "categor_list",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Set<Category> categories;

    // Avoid EAGER to prevent cascaded variant + advertise loading storms; annotate ignore to avoid recursion
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Set<Advertise> advertises;

    // Critical: changing to LAZY so that loading a Product (via ProductVariant fetch join) does not trigger
    // separate selects for all productVariants (which we already have). Prevents repeated queries seen in logs.
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Set<ProductVariant> productVariants;
}