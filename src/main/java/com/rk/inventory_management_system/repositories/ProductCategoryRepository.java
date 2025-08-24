package com.rk.inventory_management_system.repositories;

import com.rk.inventory_management_system.entities.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {


    Optional<ProductCategory> findByName(String name);

}
