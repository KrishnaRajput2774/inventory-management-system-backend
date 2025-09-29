package com.rk.inventory_management_system.dtos.supplierDtos;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierResponseDto {

    private Long id;
    private String name;
    private String contactNumber;
    private String email;
    private String address;
    private Integer productsCount;
    private LocalDateTime createdAt;

    private List<SupplierProductsResponseDto> products;
}