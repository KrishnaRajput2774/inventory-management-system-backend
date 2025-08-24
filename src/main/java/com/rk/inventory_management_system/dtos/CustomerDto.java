package com.rk.inventory_management_system.dtos;

import com.rk.inventory_management_system.entities.Order;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {

    private Long customerId;
    private String name;
    private String contactNumber;
    private String email;
    private String address;
    private List<OrderDto> orders;

}
