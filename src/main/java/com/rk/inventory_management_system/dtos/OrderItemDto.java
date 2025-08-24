package com.rk.inventory_management_system.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rk.inventory_management_system.entities.Order;
import com.rk.inventory_management_system.entities.Product;
import jakarta.persistence.ManyToOne;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {

    private Long orderItemId;
    private ProductDto productDto;   // which product is ordered
    private OrderDto orderDto;       // to know which order did this order item belongs
    private Double priceAtOrderTime; // what was the price when ordered no need if we are doing it offline
    private Integer quantity;
}
