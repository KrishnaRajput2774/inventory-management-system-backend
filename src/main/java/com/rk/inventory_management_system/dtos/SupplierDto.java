package com.rk.inventory_management_system.dtos;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierDto {

    private Long id;
    private String name;
    private String contactNumber;
    private String email;
    private String address;
    private Integer productsCount;
    private List<ProductDto> products;
    private LocalDateTime createdAt;

}
