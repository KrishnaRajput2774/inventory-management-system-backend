package com.rk.inventory_management_system.dtos;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.rk.inventory_management_system.dtos.ProductDtos.ProductSupplierResponseDto;
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
    private String productCode;
    private String attribute;
    private String name;
    private String description;
    private Double actualPrice;
    private Double sellingPrice;
    private Double discount;
    private Integer stockQuantity;
    private Integer quantitySold;
    private int lowStockThreshold;
    private ProductCategoryDto category;
    private ProductSupplierResponseDto supplier;
    private String brandName;

}
