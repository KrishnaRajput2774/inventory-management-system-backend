package com.rk.inventory_management_system.tools;


import com.rk.inventory_management_system.entities.Product;
import com.rk.inventory_management_system.repositories.ProductRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryTools {

    private final ProductRepository productRepository;


    @Tool("Find products by name or partial name match")
    public String findProductByName(@P("product name or partial name")String productName) {
        log.info("Searching for products with name containing: {}", productName);
        List<Product> products = productRepository.findByNameContainingIgnoreCase(productName);

        if (products.isEmpty()) {
            return "No products found with name containing: " + productName;
        }

        return products.stream()
                .map(this::formatProduct)
                .collect(Collectors.joining("\n"));

    }

    @Tool("Get low stock products based on threshold")
    public String getLowStockProducts() {
        log.info("Retrieving low stock products");
        List<Product> lowStockProducts = productRepository.findByStockQuantityLessThanThreshold();

        if(lowStockProducts.isEmpty()) {
            return "No products are currently low in stock.";
        }

        return "Low stock Products: \n"+
                lowStockProducts.stream().map(this::formatProductWithStock)
                        .collect(Collectors.joining("\n"));

    }

    @Tool("Get product details by product code")
    public String getProductByCode(@P("product code") String productCode) {
        log.info("Searching for product with code: {}", productCode);
        Product product = productRepository.findByProductCode(productCode);
        return formatProductDetailed(product);

    }



    private String formatProduct(Product product) {
        return String.format("Product: %s (%s) - Price: $%.2f, Stock: %d, Supplier: %s",
                product.getName(), product.getProductCode(),
                product.getSellingPrice(), product.getStockQuantity(),
                product.getSupplier().getName());
    }

    private String formatProductWithStock(Product product) {
        return String.format("Product: %s (%s) - Stock: %d (Threshold: %d)",
                product.getName(), product.getProductCode(),
                product.getStockQuantity(), product.getLowStockThreshold());
    }

    private String formatProductDetailed(Product product) {
        return String.format("Product Details:\n" +
                        "- Name: %s\n" +
                        "- Code: %s\n" +
                        "- Brand: %s\n" +
                        "- Description: %s\n" +
                        "- Price: $%.2f\n" +
                        "- Stock: %d\n" +
                        "- Category: %s\n" +
                        "- Supplier: %s",
                product.getName(), product.getProductCode(),
                product.getBrandName(), product.getDescription(),
                product.getSellingPrice(), product.getStockQuantity(),
                product.getCategory() != null ? product.getCategory().getName() : "N/A",
                product.getSupplier() != null ? product.getSupplier().getName() : "N/A");
    }











}
