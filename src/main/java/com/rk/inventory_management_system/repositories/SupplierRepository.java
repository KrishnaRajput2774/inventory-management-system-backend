package com.rk.inventory_management_system.repositories;

import com.rk.inventory_management_system.entities.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByEmail(String email);

    Optional<Supplier> findByEmailOrContactNumber(String email, String contactNumber);
}
