package com.rk.inventory_management_system.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jdk.jfr.Category;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "supplier_id","brand_name"})
)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(nullable = false)
    private String brandName;

    private Double price;

    private Double discount;

    private Integer stockQuantity;

    @ManyToOne
    private ProductCategory category;

    @ManyToOne
    @JsonBackReference
    private Supplier supplier;


}
