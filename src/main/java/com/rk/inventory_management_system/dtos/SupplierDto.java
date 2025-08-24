package com.rk.inventory_management_system.dtos;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.rk.inventory_management_system.entities.Product;
import lombok.*;

import java.util.List;
import java.util.Set;

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
    @JsonManagedReference
    private List<ProductDto> products;

}
