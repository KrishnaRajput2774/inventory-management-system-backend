package com.rk.inventory_management_system.dtos;

import com.rk.inventory_management_system.dtos.OrderITemDto.OrderItemProductDto;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
// ---- OrderItemDto ----
public class OrderItemDto {

    private Long orderItemId;
    private OrderItemProductDto productDto;   // which product is ordered
    private OrderDto orderDto;       // back reference to order

    private Double priceAtOrderTime;
    private Integer quantity;
}
