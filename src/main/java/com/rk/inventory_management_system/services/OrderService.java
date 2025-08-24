package com.rk.inventory_management_system.services;

import com.rk.inventory_management_system.dtos.OrderDto;
import com.rk.inventory_management_system.entities.Enums.OrderStatus;
import com.rk.inventory_management_system.entities.Order;

import java.util.List;

public interface OrderService {

    OrderDto createOrder(OrderDto orderDto);

    OrderDto cancelOrder(Long orderId);

    OrderDto completeOrder(Long orderId);

    List<OrderDto> getAllOrdersOfCustomer(Long customerId);

    OrderDto getOrderById(Long orderId);

    List<Order> getOrdersByIds(List<Long> orderIds);

}
