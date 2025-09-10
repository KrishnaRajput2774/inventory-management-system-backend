package com.rk.inventory_management_system.controllers;

import com.rk.inventory_management_system.dtos.OrderDto;
import com.rk.inventory_management_system.entities.Enums.OrderStatus;
import com.rk.inventory_management_system.entities.Order;
import com.rk.inventory_management_system.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;


    @GetMapping("/all")
    public ResponseEntity<List<OrderDto>> findAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PostMapping(path = "/create")
    public ResponseEntity<OrderDto> createOrder(@RequestBody OrderDto orderDto) {
        log.info("");
        return ResponseEntity.ok(orderService.createOrder(orderDto));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderDto> cancelOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.cancelOrder(orderId));
    }

    @PostMapping("/{orderId}/complete")
    public ResponseEntity<OrderDto> completeOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.completeOrder(orderId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderDto>> getAllOrdersOfCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(orderService.getAllOrdersOfCustomer(customerId));
    }

    @PostMapping("/{orderId}/status")
    public ResponseEntity<String> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String orderStatus) {

        orderService.updateOrderStatus(orderId, orderStatus);
        return ResponseEntity.ok("Order status updated successfully to " + orderStatus);
    }
}
