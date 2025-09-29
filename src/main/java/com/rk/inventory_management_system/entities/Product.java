package com.rk.inventory_management_system.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
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


    @Column(name = "product_code")
    private String productCode;

    private String attribute;

    private String name;

    @Column(nullable = false, name = "brand_name")
    private String brandName;

    private String description;

    @Column(name = "actual_price")
    private Double actualPrice;
    @Column(name = "selling_price")
    private Double sellingPrice;

    private Double discount;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "quantity_sold")
    private Integer quantitySold = 0;

    @ManyToOne
    @JsonBackReference("category-products")
    private ProductCategory category;

    @ManyToOne
    @JsonBackReference
    private Supplier supplier;

    @Column(name = "low_stock_threshold")
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