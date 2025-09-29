package com.rk.inventory_management_system.controllers;


import com.rk.inventory_management_system.dtos.OrderItemDto;
import com.rk.inventory_management_system.dtos.RemoveOrderItemRequestDto;
import com.rk.inventory_management_system.dtos.ResponseDtos.OrderResponseDto;
import com.rk.inventory_management_system.services.OrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orderItem")
public class OrderItemController {

    private final OrderItemService orderItemService;


    @PostMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> addItemToOrder(@PathVariable Long orderId,
                                                           @RequestBody OrderItemDto orderItemDto) {

        return ResponseEntity.ok(orderItemService.addItemToOrder(orderId, orderItemDto));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<List<OrderItemDto>> removeItemFromOrder(@PathVariable Long orderId,
                                                                  @RequestBody RemoveOrderItemRequestDto itemRequestDto) {

        return ResponseEntity.ok(orderItemService.removeItemFromOrder(
                orderId,
                itemRequestDto.getOrderItemId(),
                itemRequestDto.getQuantityToRemove()));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getAllItemsByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderItemService.getAllItemsByOrder(orderId));
    }










}
