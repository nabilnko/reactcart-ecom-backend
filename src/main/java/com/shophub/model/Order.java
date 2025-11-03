package com.shophub.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String userName;
    private String userEmail;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String district;

    @Column(nullable = false)
    private Double total;

    @Column(nullable = false)
    private String status = "pending";

    private String paymentMethod;
    private String deliveryMethod;
    private Double deliveryCharge;
    private String comment;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference  // Add this annotation
    private List<OrderItem> items;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
