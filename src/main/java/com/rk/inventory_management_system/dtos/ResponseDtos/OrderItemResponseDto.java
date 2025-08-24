package com.rk.inventory_management_system.dtos.ResponseDtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponseDto {

    private Long id;

    private Long productId;
    private String productName;
    private String brand;
    private Double price;
    private Double discount=0.0;
    private Integer quantity;

    private Double totalPrice;
}
