package com.rk.inventory_management_system.dtos;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
