package com.rk.inventory_management_system.services;

import com.rk.inventory_management_system.dtos.OrderItemDto;
import com.rk.inventory_management_system.dtos.ResponseDtos.OrderResponseDto;

import java.util.List;

public interface OrderItemService {

     OrderResponseDto addItemToOrder(Long orderId, OrderItemDto orderItemDto);

     List<OrderItemDto> removeItemFromOrder(Long orderId, Long orderItemId, Integer quantityToRemove);

     OrderResponseDto getAllItemsByOrder(Long orderId);

//     OrderItem getOrderItemById(Long orderItemId);
}
