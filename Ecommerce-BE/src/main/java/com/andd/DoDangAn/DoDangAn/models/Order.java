package com.andd.DoDangAn.DoDangAn.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="id")
    private String id;

    @ManyToOne
    @JoinColumn(name = "userId")
    private User user;

    @ManyToOne
    @JoinColumn(name = "productId")
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "totalPrice", nullable = false)
    private Double totalPrice;

    @Column(name = "addedDate", nullable = false)
    private LocalDateTime addedDate;

    @ManyToOne
    @JoinColumn(name = "orderInfo", nullable = false)
    private OrderInfo orderInfo;
}