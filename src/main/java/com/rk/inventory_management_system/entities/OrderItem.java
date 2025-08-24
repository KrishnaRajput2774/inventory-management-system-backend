package com.rk.inventory_management_system.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    private LocalDateTime orderDate;

    @ManyToOne(fetch = FetchType.EAGER)
    private Product product;  // which product is ordered

    @ManyToOne
    private Order order;       // to know which order did this order item belongs

    private Double priceAtOrderTime; // what was the price when ordered no need if we are doing it offline

    private Integer quantity;  // how many units of this product in the order
}
