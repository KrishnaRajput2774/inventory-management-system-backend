package com.rk.inventory_management_system.services.impl;

import com.rk.inventory_management_system.dtos.ProductDto;
import com.rk.inventory_management_system.dtos.SupplierDto;
import com.rk.inventory_management_system.entities.Product;
import com.rk.inventory_management_system.entities.Supplier;
import com.rk.inventory_management_system.exceptions.ResourceNotFoundException;
import com.rk.inventory_management_system.exceptions.RuntimeConflictException;
import com.rk.inventory_management_system.repositories.SupplierRepository;
import com.rk.inventory_management_system.services.SupplierService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierServiceImpl implements SupplierService {
    private final ModelMapper modelMapper;

    private final SupplierRepository supplierRepository;


    @Override
    @Transactional
    public SupplierDto createSupplier(SupplierDto supplierDto) {

        Supplier existingSupplier = supplierRepository
                .findByEmailOrContactNumber(supplierDto.getEmail(), supplierDto.getContactNumber())
                .orElse(null);

        if (existingSupplier != null) {
            throw new RuntimeConflictException("Supplier already exists with the same email or contact number.");  // supplier already exists
        }

        Supplier supplier = Supplier.builder()
                .name(supplierDto.getName())
                .address(supplierDto.getAddress())
                .email(supplierDto.getEmail())
                .contactNumber(supplierDto.getContactNumber())
                .build();

        List<ProductDto> productsDto = supplierDto.getProducts();

        if(productsDto != null){
            List<Product> products = productsDto.stream()
                    .map(productDto -> {
                        Product product = modelMapper.map(productDto, Product.class);
                        product.setSupplier(supplier);
                        return product;
                    })
                    .toList();
            supplier.setProducts(products);
        }
        else {
            supplier.setProducts(null);
        }

        return modelMapper.map(supplierRepository.save(supplier), SupplierDto.class);
    }

    @Override
    @Transactional
    public SupplierDto deleteSupplier(Long supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier Not Found with Id: " + supplierId));
        supplierRepository.delete(supplier);
        return modelMapper.map(supplier, SupplierDto.class);
    }

    @Override
    @Transactional
    public List<ProductDto> getAllProductsOfSupplier(Long supplierId) {

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(()->
                        new ResourceNotFoundException("Supplier Not Found with Id: "+supplierId));


        log.info("Fetching all products for supplierId={} (total: {})", supplierId, supplier.getProducts().size());
        List<Product> products = supplier.getProducts();

        return products.stream()
                .map(product ->
                        modelMapper.map(product, ProductDto.class))
                .toList();
    }

    @Override
    public List<SupplierDto> getAllSuppliers() {

        List<Supplier> suppliers = supplierRepository.findAll();
        return suppliers.stream()
                .map((supplier) -> modelMapper.map(supplier, SupplierDto.class))
                .toList();
    }

    @Override
    public Supplier getSupplier(String email, String contact_number) {
        return supplierRepository.findByEmailOrContactNumber(email,contact_number)
                .orElse(null);
    }

    @Override
    public Supplier getSupplierById(Long supplierId) {
        return supplierRepository.findById(supplierId)
                .orElse(null);
    }


}
