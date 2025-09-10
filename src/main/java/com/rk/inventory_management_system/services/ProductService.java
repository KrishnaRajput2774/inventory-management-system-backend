package com.rk.inventory_management_system.services;

import com.rk.inventory_management_system.dtos.ProductDto;
import com.rk.inventory_management_system.dtos.ProductDtos.ProductResponseDto;
import com.rk.inventory_management_system.dtos.ProductStockResponseDto;
import com.rk.inventory_management_system.entities.Product;
import com.rk.inventory_management_system.entities.Supplier;

import java.util.List;
import java.util.Optional;

public interface    ProductService {

    ProductResponseDto createProduct(ProductDto productDto);

    ProductResponseDto getProductById(Long id);

    List<ProductDto> getAllProducts();
    //will show all the products, means same product will be shown with different supplier
    //TODO make method that only show Products with different name and brand not with different supplier

    ProductStockResponseDto getStockOfProduct(Long productId);

    ProductDto increaseStockOfProduct(Long productId, Integer quantityToAdd);

    List<ProductDto> reduceStockOfProduct(Long productId, Integer quantity);

    Product getProductByNameAndSupplier(String productName, Supplier supplier);

    Optional<Product> getProductByIdWithLock(Long productId);

    List<Product> findProductByNameAndBrandName(String name, String brandName);

    void saveAll(List<Product> products);

}
