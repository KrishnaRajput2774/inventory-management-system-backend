package com.rk.inventory_management_system.dtos.CategoryDtos;

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
public class ProductCategoryResponseDto {

    private Long productId;
    private String name;
    private String productCode;
    private String description;
    private Double sellingPrice;
    private Double actualPrice;
    private Double discount;
    private Integer stockQuantity;
    private Integer quantitySold;
    private int lowStockThreshold;
    private SupplierDto supplier;
    private String brandName;


}
