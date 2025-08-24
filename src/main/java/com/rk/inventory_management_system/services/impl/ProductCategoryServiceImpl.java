package com.rk.inventory_management_system.services.impl;

import com.rk.inventory_management_system.dtos.ProductCategoryDto;
import com.rk.inventory_management_system.dtos.ProductDto;
import com.rk.inventory_management_system.entities.Product;
import com.rk.inventory_management_system.entities.ProductCategory;
import com.rk.inventory_management_system.exceptions.ResourceNotFoundException;
import com.rk.inventory_management_system.exceptions.RuntimeConflictException;
import com.rk.inventory_management_system.repositories.ProductCategoryRepository;
import com.rk.inventory_management_system.services.ProductCategoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCategoryServiceImpl implements ProductCategoryService {

    private final ProductCategoryRepository productCategoryRepository;
    private final ModelMapper modelMapper;


    @Override
    @Transactional
    public ProductCategoryDto createProductCategory(ProductCategoryDto categoryDto) {

        ProductCategory category = productCategoryRepository.findByName(categoryDto.getName())
                .orElse(null);

        if (category != null) {
            throw new RuntimeConflictException("Category Already Present with id: " + category.getId());
        }

        categoryDto.setId(null);
        ProductCategory savedProductCategory = productCategoryRepository.save(modelMapper.map(categoryDto, ProductCategory.class));
        return modelMapper.map(savedProductCategory, ProductCategoryDto.class);
    }

    @Override
    @Transactional
    public ProductCategoryDto deleteProductCategory(Long id) {

        log.info("Deleting Product Category with Id: " + id);

        ProductCategory category = productCategoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product Category Not Found with Id " + id));

        productCategoryRepository.delete(category);

        log.info(" Product Category with Id: " + id + " was Deleted Successfully");
        return modelMapper.map(category, ProductCategoryDto.class);
    }

    @Override
    public List<ProductCategoryDto> getAllCategories() {
        log.info("Fetching All Categories");
        List<ProductCategory> categories = productCategoryRepository.findAll();

        log.info("Fetched All Categories");
        return categories.stream()
                .map(category->modelMapper.map(category, ProductCategoryDto.class))
                .toList();
    }

    @Override
    public ProductCategoryDto getCategoryById(Long id) {

        log.info("Fetching Category with Id: "+id);
        ProductCategory category = productCategoryRepository.findById(id)
                .orElseThrow(()->
                        new ResourceNotFoundException("Product Category Not Found with Id " + id)
                );

        log.info("Fetched Category: "+category.getName());
        return modelMapper.map(category, ProductCategoryDto.class);
    }

    @Override
    public List<ProductDto> getProductsByCategory(Long categoryId) {

        log.info("Fetching Products of Category with Id: "+categoryId);
        ProductCategory category = productCategoryRepository.findById(categoryId)
                .orElseThrow(()->
                new ResourceNotFoundException("Product Category Not Found with Id " + categoryId)
        );


        List<Product> products = category.getProducts();
        log.info("Products of Category: "+category.getName()+" Fetched Successfully");


        return products.stream().map(
                product -> modelMapper.map(product, ProductDto.class))
                .toList();
    }
}
