package com.rk.inventory_management_system.controllers;


import com.rk.inventory_management_system.dtos.ProductDto;
import com.rk.inventory_management_system.dtos.ProductDtos.ProductResponseDto;
import com.rk.inventory_management_system.dtos.ProductStockResponseDto;
import com.rk.inventory_management_system.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ModelMapper modelMapper;


    @PostMapping("/create")
    public ResponseEntity<ProductResponseDto> createProduct(@RequestBody ProductDto productDto) {

        ProductResponseDto dto = ProductResponseDto.builder()
                .productCode(productDto.getProductCode())
                .productId(productDto.getProductId())
                .name(productDto.getName())
                .actualPrice(productDto.getActualPrice())
                .sellingPrice(productDto.getSellingPrice())
                .discount(productDto.getDiscount())
                .lowStockThreshold(productDto.getLowStockThreshold())
                .quantitySold(productDto.getStockQuantity())
                .stockQuantity(productDto.getStockQuantity())
                .attribute(productDto.getAttribute())
                .category(productDto.getCategory())
                .description(productDto.getDescription())
                .brandName(productDto.getBrandName())
                .supplier(productDto.getSupplier())
                .build();

        return ResponseEntity.ok(productService.createProduct(dto));
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProductById(productId));
    }

    @GetMapping("/{productId}/stock")
    public ResponseEntity<ProductStockResponseDto> getStockOfProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getStockOfProduct(productId));
    }

    @PostMapping("/{productId}/stock/increase/{quantityToAdd}")
    public ResponseEntity<ProductDto> increaseStockOfProduct(
            @PathVariable Long productId,
            @PathVariable Integer quantityToAdd) {

        ProductResponseDto dto = productService.increaseStockOfProduct(productId, quantityToAdd);
        return ResponseEntity.ok(modelMapper.map(dto, ProductDto.class));
    }

    @PostMapping("/{productId}/stock/reduce/{quantityToReduce}")
    public ResponseEntity<List<ProductDto>> reduceStockOfProduct(
            @PathVariable Long productId,
            @PathVariable Integer quantityToReduce) {

        return ResponseEntity.ok(productService.reduceStockOfProduct(productId, quantityToReduce));
    }
}
