    package com.rk.inventory_management_system.dtos;

    import lombok.*;

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public class ProductCategoryDto {

        private Long id;
        private String name;
    }
