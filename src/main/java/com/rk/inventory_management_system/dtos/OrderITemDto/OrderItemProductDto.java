package com.rk.inventory_management_system.dtos.OrderITemDto;

import com.rk.inventory_management_system.dtos.OrderDto;
import com.rk.inventory_management_system.dtos.ProductCategoryDto;
import com.rk.inventory_management_system.dtos.ProductDto;
import com.rk.inventory_management_system.dtos.SupplierDto;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemProductDto {

    private Long productId;
    private String productCode;
    private String name;
    private String brandName;

    private String description;

    private Double sellingPrice;
    private Double actualPrice;
    private Double discount;

    private Integer stockQuantity;
    private Integer quantitySold;


    private ProductCategoryDto category;
    private OrderItemSupplierDto supplier;
}
