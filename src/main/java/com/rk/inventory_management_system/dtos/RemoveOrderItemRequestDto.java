package com.rk.inventory_management_system.dtos;


import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveOrderItemRequestDto {

    private Long orderItemId;
    private Integer quantityToRemove;

}
