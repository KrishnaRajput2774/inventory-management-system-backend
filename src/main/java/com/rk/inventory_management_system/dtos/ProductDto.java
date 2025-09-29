package com.rk.inventory_management_system.dtos;

import com.rk.inventory_management_system.dtos.ProductDtos.ProductSupplierResponseDto;
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
