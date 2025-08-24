package com.rk.inventory_management_system.dtos.ResponseDtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {

    private Long orderId;

    private Long customerId;
    private String customerName;

    private List<OrderItemResponseDto> items;

    private Double totalPrice; // sum of all items totalPrice


}
