package com.rk.inventory_management_system.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jdk.jfr.Category;
import lombok.*;

import java.util.Arrays;
import java.util.stream.Collectors;

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
    private Long id; //db level


    private String productCode;

    private String attribute;

    private String name;

    @Column(nullable = false)
    private String brandName;

    private String description;

    private Double actualPrice;
    private Double sellingPrice;

    private Double discount;

    private Integer stockQuantity;
    private Integer quantitySold = 0;

    @ManyToOne
    private ProductCategory category;

    @ManyToOne
    @JsonBackReference
    private Supplier supplier;

    private int lowStockThreshold = 10;

    @PostPersist
    public void generateCodeAfterPersist() {
        if (productCode == null || productCode.isEmpty()) {
            String namePart = name.replaceAll("\\s+", "")
                    .substring(0, Math.min(4, name.length()))
                    .toUpperCase();

            String categoryPart = category.getName()
                    .substring(0, Math.min(3, category.getName().length()))
                    .toUpperCase();

            String supplierPart = supplier.getId().toString();

            String attrPart = (attribute != null && !attribute.isEmpty())
                    ? Arrays.stream(attribute.split("\\s+"))
                    .map(word -> word.substring(0, Math.min(3, word.length())).toUpperCase())
                    .collect(Collectors.joining("-"))
                    : "NA";

            this.productCode = String.format("%s-%d-%s-%s-%s",
                    namePart, id, categoryPart, supplierPart, attrPart);
        }
    }

}