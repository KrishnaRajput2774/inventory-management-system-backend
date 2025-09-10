package com.rk.inventory_management_system.dtos.supplierDtos;

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
public class SupplierProductsResponseDto {
    private Long id;
    private Long productId;
    private String name;
    private String productCode;
    private String description;
    private Double actualPrice;
    private Double sellingPrice;
    private int lowStockThreshold;
    private Double discount;
    private Integer stockQuantity;
    private Integer quantitySold;

    private ProductCategoryDto category;
    private String brandName;
}
