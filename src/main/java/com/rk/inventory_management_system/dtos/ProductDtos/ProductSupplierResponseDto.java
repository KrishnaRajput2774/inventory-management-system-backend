package com.rk.inventory_management_system.dtos.ProductDtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductSupplierResponseDto {
    private Long id;
    private String name;
    private String contactNumber;
    private String email;
    private String address;
    private Integer productsCount;
    private LocalDateTime createdAt;

}
