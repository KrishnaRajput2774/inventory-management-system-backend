package com.rk.inventory_management_system.dtos;

import com.rk.inventory_management_system.entities.Customer;
import com.rk.inventory_management_system.entities.Enums.OrderStatus;
import com.rk.inventory_management_system.entities.Enums.OrderType;
import com.rk.inventory_management_system.entities.Enums.PaymentType;
import com.rk.inventory_management_system.entities.OrderItem;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {


    private Long orderId;
    private OrderType orderType;
    private OrderStatus orderStatus;
    private PaymentType paymentType;
    private CustomerDto customerDto;
    private SupplierDto supplierDto;
    private List<OrderItemDto> orderItems;
    private Double totalPrice;



}
