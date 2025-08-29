package com.andd.DoDangAn.DoDangAn.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "advertises")
@Data
public class Advertise {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String title;

    private String description;

    private String imageUrl;

    @Column(name = "date")
    private LocalDateTime date;

    private String role;

    @ManyToOne
    @JoinColumn(name = "categoryID")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "productID")
    private Product product;
}