package com.rk.inventory_management_system.dtos;


import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class ProductStockDetailsDto {

    private Long productId;
    private Integer stockQuantity;
    private Integer quantitySold;
    private int lowStockThreshold;
    private Long supplierId;
    private String supplierName;

}
