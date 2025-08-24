package com.rk.inventory_management_system.services;

import com.rk.inventory_management_system.dtos.ProductDto;
import com.rk.inventory_management_system.dtos.SupplierDto;
import com.rk.inventory_management_system.entities.Supplier;

import java.util.List;
import java.util.Set;

public interface SupplierService {

    SupplierDto createSupplier(SupplierDto supplierDto);

    SupplierDto deleteSupplier(Long supplierId);

    List<ProductDto> getAllProductsOfSupplier(Long supplierId);

    List<SupplierDto> getAllSuppliers();

    Supplier getSupplier(String email, String contact_number);

    Supplier getSupplierById(Long supplierId);

}
