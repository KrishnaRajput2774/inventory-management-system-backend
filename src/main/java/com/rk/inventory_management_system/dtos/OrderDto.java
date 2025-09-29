package com.rk.inventory_management_system.dtos;

import com.rk.inventory_management_system.entities.Enums.OrderStatus;
import com.rk.inventory_management_system.entities.Enums.OrderType;
import com.rk.inventory_management_system.entities.Enums.PaymentType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderDto {

    private Long orderId;
    private LocalDateTime createdAt;
    private OrderType orderType;
    private OrderStatus orderStatus;
    private PaymentType paymentType;
    private CustomerDto customer;
    private SupplierDto supplier;
    private List<OrderItemDto> orderItems;
    private Double totalPrice;
}
