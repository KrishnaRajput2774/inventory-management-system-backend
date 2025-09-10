package com.rk.inventory_management_system.controllers;


import com.rk.inventory_management_system.dtos.ProductDto;
import com.rk.inventory_management_system.dtos.ProductDtos.ProductResponseDto;
import com.rk.inventory_management_system.dtos.ProductStockResponseDto;
import com.rk.inventory_management_system.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;


    @PostMapping("/create")
    public ResponseEntity<ProductResponseDto> createProduct(@RequestBody ProductDto productDto) {
        return ResponseEntity.ok(productService.createProduct(productDto));
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

         return ResponseEntity.ok(productService.increaseStockOfProduct(productId, quantityToAdd));
    }

    @PostMapping("/{productId}/stock/reduce/{quantityToReduce}")
    public ResponseEntity<List<ProductDto>> reduceStockOfProduct(
            @PathVariable Long productId,
            @PathVariable Integer quantityToReduce) {

        return ResponseEntity.ok(productService.reduceStockOfProduct(productId, quantityToReduce));
    }
}
