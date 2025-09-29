package com.rk.inventory_management_system.dtos.OrderITemDto;

import com.rk.inventory_management_system.dtos.ProductCategoryDto;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class    OrderItemProductDto {

    private Long productId;
    private String productCode;
    private String name;
    private String brandName;
    private String attribute;

    private Integer lowStockThreshold;
    private String description;

    private Double sellingPrice;
    private Double actualPrice;
    private Double discount;

    private Integer stockQuantity;
    private Integer quantitySold;


    private ProductCategoryDto category;
    private OrderItemSupplierDto supplier;
}
