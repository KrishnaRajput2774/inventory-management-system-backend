package com.rk.inventory_management_system.dtos;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class ProductStockResponseDto {

    private String productName;
    private List<ProductStockDetailsDto> productStockDetails;
    private Integer totalStock;
}
