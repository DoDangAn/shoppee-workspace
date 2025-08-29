package com.andd.DoDangAn.DoDangAn.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "likings")
@Data
public class Likings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
}