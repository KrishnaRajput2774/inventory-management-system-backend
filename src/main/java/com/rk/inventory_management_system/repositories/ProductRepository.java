package com.rk.inventory_management_system.repositories;

import com.rk.inventory_management_system.entities.Product;
import com.rk.inventory_management_system.entities.Supplier;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {


    Optional<Product> findByNameAndSupplier(String name, Supplier supplier);

    List<Product> findByNameAndBrandName(String name, String brandName);

    Optional<Product> findByNameAndBrandNameAndSupplier(String name, String brandName, Supplier supplier);

    Optional<List<Product>> findByName(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(Long id);
}
