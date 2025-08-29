package com.andd.DoDangAn.DoDangAn.models;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_varients")
@Data
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "pVarient_id")
    private String id;


    @Column(name = "productName")
    private String productName;

    private String description;

    private Integer quantity;

    private Double price;

    @Column(name = "newPrice")
    private Double newPrice;

    @Column(name = "date")
    private LocalDateTime date;
    @Column(name="viewCount")
    private Integer viewCount;
    private String imageUrl;

    private String videoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "product")
    @JsonIgnore // Tránh lỗi lazy proxy khi serialize ngoài session
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "category_id")
    @JsonIgnore
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "country")
    @JsonIgnore
    private Country country;


    @Column(name="videoPublicIds")
    private String videoPublicIds;

}