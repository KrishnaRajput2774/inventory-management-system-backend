package com.rk.inventory_management_system.dtos.ProductDtos;

import com.rk.inventory_management_system.dtos.ProductCategoryDto;
import com.rk.inventory_management_system.dtos.SupplierDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductResponseDto {

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
