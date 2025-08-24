package com.rk.inventory_management_system.services;

import com.rk.inventory_management_system.dtos.ProductCategoryDto;
import com.rk.inventory_management_system.dtos.ProductDto;
import com.rk.inventory_management_system.entities.Product;
import com.rk.inventory_management_system.entities.ProductCategory;
import jdk.jfr.Category;

import java.util.List;

public interface ProductCategoryService {


    ProductCategoryDto createProductCategory(ProductCategoryDto categoryDto);

    ProductCategoryDto deleteProductCategory(Long id);

    List<ProductCategoryDto> getAllCategories();

    ProductCategoryDto getCategoryById(Long id);

    List<ProductDto> getProductsByCategory(Long categoryId);

}
