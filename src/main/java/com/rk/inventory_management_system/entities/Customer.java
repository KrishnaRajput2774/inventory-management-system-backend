package com.rk.inventory_management_system.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "contact_number")
    private String contactNumber;

    private String email;

    private String address;

    @OneToMany(mappedBy = "customer")
    private List<Order> order;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
