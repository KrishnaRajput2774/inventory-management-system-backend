package com.rk.inventory_management_system.controllers;

import com.rk.inventory_management_system.dtos.CustomerDto;
import com.rk.inventory_management_system.dtos.ProductDto;
import com.rk.inventory_management_system.dtos.SupplierDto;
import com.rk.inventory_management_system.entities.Supplier;
import com.rk.inventory_management_system.services.SupplierService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<SupplierDto>> getAllSuppliers() {

        return ResponseEntity.ok(supplierService.getAllSuppliers());
    }

    @GetMapping("/{supplierId}/products/all")
    public ResponseEntity<List<ProductDto>> getAllProductsOfSupplier(@PathVariable Long supplierId) {
        return ResponseEntity.ok(supplierService.getAllProductsOfSupplier(supplierId));
    }

    @GetMapping("/{supplierId}")
    public ResponseEntity<SupplierDto> getSupplierById(@PathVariable Long supplierId) {

        return ResponseEntity.ok(modelMapper.map(
                supplierService.getSupplierById(supplierId),
                SupplierDto.class
        ));
    }






}
