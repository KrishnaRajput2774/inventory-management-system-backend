package com.rk.inventory_management_system.services.impl;

import com.rk.inventory_management_system.entities.Product;
import com.rk.inventory_management_system.repositories.ProductRepository;
import com.rk.inventory_management_system.services.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockServiceImpl implements StockService {

    private final ProductRepository productRepository;

    @Override
    public List<Product> getLowStockProducts() {
        return productRepository.findByStockQuantityLessThanThreshold();
    }
}
