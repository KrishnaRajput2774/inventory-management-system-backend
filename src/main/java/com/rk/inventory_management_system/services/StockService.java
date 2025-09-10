package com.rk.inventory_management_system.services;

import com.rk.inventory_management_system.entities.Product;

import java.util.List;

public interface StockService {

    public List<Product> getLowStockProducts();


}
