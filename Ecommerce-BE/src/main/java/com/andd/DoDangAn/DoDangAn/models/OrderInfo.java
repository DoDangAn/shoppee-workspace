package com.andd.DoDangAn.DoDangAn.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "order_info")
@Data
public class OrderInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "orderId")
    private String Id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private Long amount;

    private String status;

    private String orderInfo;

    private Instant createdDate;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @OneToMany(mappedBy = "orderInfo")
    private Set<Order> orders = new LinkedHashSet<>();
}