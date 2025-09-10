package com.rk.inventory_management_system.controllers;

import com.rk.inventory_management_system.dtos.CustomerDto;
import com.rk.inventory_management_system.dtos.ProductDto;
import com.rk.inventory_management_system.dtos.SupplierDto;
import com.rk.inventory_management_system.dtos.supplierDtos.SupplierProductsResponseDto;
import com.rk.inventory_management_system.dtos.supplierDtos.SupplierResponseDto;
import com.rk.inventory_management_system.entities.Supplier;
import com.rk.inventory_management_system.services.SupplierService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/supplier")
public class SupplierController {

    private final SupplierService supplierService;
    private final ModelMapper modelMapper;


    @PostMapping("/create")
    public ResponseEntity<SupplierDto> createSupplier(@RequestBody SupplierDto supplierDto) {
        return ResponseEntity.ok(supplierService.createSupplier(supplierDto));
    }

    @DeleteMapping("{supplierId}")
    public ResponseEntity<SupplierDto> deleteSupplier(@PathVariable Long supplierId) {
        return ResponseEntity.ok(supplierService.deleteSupplier(supplierId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<SupplierResponseDto>> getAllSuppliers() {

        return ResponseEntity.ok(supplierService.getAllSuppliers());
    }

    @GetMapping("/{supplierId}/products")
    public ResponseEntity<List<SupplierProductsResponseDto>> getAllProductsOfSupplier(@PathVariable Long supplierId) {
        return ResponseEntity.ok(supplierService.getAllProductsOfSupplier(supplierId));
    }

    @GetMapping("/{supplierId}")
    public ResponseEntity<SupplierResponseDto> getSupplierById(@PathVariable Long supplierId) {

        Supplier supplier = supplierService.getSupplierById(supplierId);
         SupplierResponseDto supplierResponseDto = modelMapper.map(
                supplier,
                SupplierResponseDto.class
        );
         supplierResponseDto.setProducts(supplier.getProducts().stream().map((element) -> modelMapper.map(element, SupplierProductsResponseDto.class)).collect(Collectors.toList()));

        return ResponseEntity.ok(supplierResponseDto);

    }






}
