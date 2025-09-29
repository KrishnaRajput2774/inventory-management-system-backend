package com.rk.inventory_management_system.dtos;

import lombok.*;

import java.time.LocalDate;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProductCategoryDto {

    private Long id;
    private String name;
    private LocalDate createdDate;
    private String description;

}
