package com.rk.inventory_management_system.dtos.OrderITemDto;

import com.rk.inventory_management_system.dtos.ProductDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemSupplierDto {
    private Long id;
    private String name;
    private String contactNumber;
    private String email;
    private String address;
    private Integer productsCount;
    private LocalDateTime createdAt;

}