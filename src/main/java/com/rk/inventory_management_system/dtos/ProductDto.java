package com.rk.inventory_management_system.dtos;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.rk.inventory_management_system.entities.ProductCategory;
import com.rk.inventory_management_system.entities.Supplier;
import jakarta.persistence.ManyToOne;
import jdk.jfr.Category;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

    private Long productId;
    private String name;
    private Double price;
    private Double discount;
    private Integer stockQuantity;
    private ProductCategoryDto category;
    @JsonBackReference
    private SupplierDto supplier;
    private String brandName;

}
