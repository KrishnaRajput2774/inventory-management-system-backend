package com.rk.inventory_management_system.dtos;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rk.inventory_management_system.dtos.OrderITemDto.OrderItemProductDto;
import com.rk.inventory_management_system.entities.Order;
import com.rk.inventory_management_system.entities.Product;
import jakarta.persistence.ManyToOne;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// ---- OrderItemDto ----
public class OrderItemDto {

    private Long orderItemId;
    private OrderItemProductDto productDto;   // which product is ordered
    private OrderDto orderDto;       // back reference to order

    private Double priceAtOrderTime;
    private Integer quantity;
}
