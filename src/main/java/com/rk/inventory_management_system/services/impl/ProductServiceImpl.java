package com.rk.inventory_management_system.services.impl;

import com.rk.inventory_management_system.dtos.ProductDto;
import com.rk.inventory_management_system.dtos.ProductStockDetailsDto;
import com.rk.inventory_management_system.dtos.ProductStockResponseDto;
import com.rk.inventory_management_system.entities.Product;
import com.rk.inventory_management_system.entities.ProductCategory;
import com.rk.inventory_management_system.entities.Supplier;
import com.rk.inventory_management_system.exceptions.InsufficientStockException;
import com.rk.inventory_management_system.exceptions.ResourceNotFoundException;
import com.rk.inventory_management_system.exceptions.RuntimeConflictException;
import com.rk.inventory_management_system.repositories.ProductRepository;
import com.rk.inventory_management_system.services.ProductCategoryService;
import com.rk.inventory_management_system.services.ProductService;
import com.rk.inventory_management_system.services.SupplierService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;
    private final SupplierService supplierService;
    private final ProductCategoryService productCategoryService;

    @Override
    @Transactional
    public ProductDto createProduct(ProductDto productDto) {

        log.info("Inside Add Product");
        Supplier supplier = supplierService.
                getSupplierById(productDto.getSupplier().getId());

        log.info("Checking supplier is present or not");
        if (supplier == null) {
            throw new ResourceNotFoundException("Supplier Not Found with Id: " + productDto.getSupplier().getId());
        }

        //Handling Category
        if (productDto.getCategory().getId() == null) {
            throw new RuntimeException("Product category must be provided");
        }

        ProductCategory category = modelMapper.map(
                productCategoryService.getCategoryById(productDto.getCategory().getId()), ProductCategory.class);

        if (category == null) {
            throw new RuntimeException("Product category not found with id: " + productDto.getCategory().getId());
        }

        Product product = productRepository.findByNameAndBrandNameAndSupplier(productDto.getName(), productDto.getBrandName(), supplier)
                .orElse(null);

        if (product != null) {
            product.setStockQuantity(product.getStockQuantity() + productDto.getStockQuantity());
        } else {
            log.info("Creating Product");
            product = new Product();
            product.setName(productDto.getName());
            product.setBrandName(productDto.getBrandName());
            product.setPrice(productDto.getPrice());
            product.setStockQuantity(productDto.getStockQuantity());

            // --- Set relationships manually ---
            product.setSupplier(supplier);
            product.setCategory(category);

            category.getProducts().add(product);
        }

        // category will be automatically saved because product is owning side
        return modelMapper.map(productRepository.save(product), ProductDto.class);
    }

    @Override
    public ProductDto getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product Not Found in Inventory with id: " + productId));

        return modelMapper.map(product, ProductDto.class);
    }

    @Override
    public List<ProductDto> getAllProducts() {

        List<Product> products = productRepository.findAll();
        return products
                .stream()
                .map((product) ->
                        modelMapper.map(product, ProductDto.class))
                .collect(Collectors.toList());
    }


    @Override
    public ProductStockResponseDto getStockOfProduct(Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product Not Found in Inventory with id: " + productId
                        ));

        List<Product> products = productRepository.findByNameAndBrandName(product.getName(), product.getBrandName());

        if (products.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No stock found for product: " + product.getName() + ", brand: " + product.getBrandName()
            );
        }

        Integer totalStock = products.stream().mapToInt(Product::getStockQuantity).sum();

        List<ProductStockDetailsDto> stockDetailsDtos = products.stream().map(product1 ->
                ProductStockDetailsDto.builder()
                        .productId(product1.getId())
                        .stockQuantity(product1.getStockQuantity())
                        .supplierId(product1.getSupplier().getId())
                        .supplierName(product1.getSupplier().getName())
                        .build()).toList();

        return ProductStockResponseDto.builder()
                .productName(product.getName())
                .productStockDetails(stockDetailsDtos)
                .totalStock(totalStock)
                .build();

    }

    @Override
    public ProductDto increaseStockOfProduct(Long productId, Integer quantityToAdd) {

        validateQuantity(quantityToAdd, true);


        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not Found in Inventory with id: " + productId
                ));

        log.info("Increasing stock for productId={} by {} units", productId, quantityToAdd);

        Product updatedProduct = updateStock(product, quantityToAdd);

        log.info("Stock updated. New quantity: {}", updatedProduct.getStockQuantity());

        return modelMapper.map(updatedProduct, ProductDto.class);
    }

    @Override
    @Transactional
    public List<ProductDto> reduceStockOfProduct(Long productId, Integer quantityToReduce) {

        validateQuantity(quantityToReduce, false);

        // Get the product with pessimistic lock to prevent race conditions
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not Found in Inventory with id: " + productId
                ));

        List<Product> products = productRepository.findByNameAndBrandName(product.getName(), product.getBrandName());
        int totalStock = products.stream().mapToInt(Product::getStockQuantity).sum();

        if (totalStock < quantityToReduce) {
            throw new InsufficientStockException(
                    String.format("Insufficient stock for product: %s , Requested: %d, Available: %d",
                            product.getName(), quantityToReduce, product.getStockQuantity()));
        }

        log.info("Reducing stock for productId={} by {} units", productId, quantityToReduce);


//        Product updatedProduct = updateStock(product, -(quantityToReduce));

        int remainingStock = quantityToReduce;
        for (Product p : products) {

            if (remainingStock <= 0) break;

            int currentProductStock = p.getStockQuantity();
            int deduction = Math.min(currentProductStock, remainingStock);

            if (deduction == 0) continue;

            p.setStockQuantity(currentProductStock - deduction);
            productRepository.save(p);

            log.info("Reduced {} units from supplier {}. Remaining: {}",
                    deduction, p.getSupplier().getName(), p.getStockQuantity());

            remainingStock -= deduction;
        }

        return products.stream().map(product1 ->
                        modelMapper.map(product1, ProductDto.class))
                .toList();
    }

    @Override
    public Product getProductByNameAndSupplier(String productName, Supplier supplier) {
        return productRepository.findByNameAndSupplier(productName, supplier)
                .orElse(null);

    }

    @Override
    public Optional<Product> getProductByIdWithLock(Long productId) {
        return productRepository.findByIdWithLock(productId);
    }

    @Override
    public List<Product> findProductByNameAndBrandName(String name, String brandName) {
        return productRepository.findByNameAndBrandName(name, brandName);
    }


    public void validateQuantity(Integer quantity, Boolean isIncrease) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity to +" + (isIncrease ? "add" : "reduce") + " must be greater than 0");
        }
    }

    public Product updateStock(Product product, Integer quantityToChange) {
        product.setStockQuantity(product.getStockQuantity() + quantityToChange);
        return productRepository.save(product);
    }
}
