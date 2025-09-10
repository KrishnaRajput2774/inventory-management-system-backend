package com.rk.inventory_management_system.controllers;

import com.rk.inventory_management_system.dtos.ProductCategoryDto;
import com.rk.inventory_management_system.dtos.ProductDto;
import com.rk.inventory_management_system.entities.ProductCategory;
import com.rk.inventory_management_system.services.ProductCategoryService;
import jdk.jfr.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/category")
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    @PostMapping("/create")
    public ResponseEntity<ProductCategoryDto> createProductCategory(@RequestBody ProductCategoryDto categoryDto) {
        return ResponseEntity.ok(productCategoryService.createProductCategory(categoryDto));
    }

    @DeleteMapping("/{categoryId}/delete")
    public ResponseEntity<ProductCategoryDto> deleteProductCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productCategoryService.deleteProductCategory(categoryId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProductCategoryDto>> getAllCategories() {
        return ResponseEntity.ok(productCategoryService.getAllCategories());
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<ProductCategoryDto> getCategoryById(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productCategoryService.getCategoryById(categoryId));
    }

    @GetMapping("/{categoryId}/products")
    public ResponseEntity<List<ProductDto>> getProductsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productCategoryService.getProductsByCategory(categoryId));
    }
}