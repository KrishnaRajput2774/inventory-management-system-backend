package com.rk.inventory_management_system.repositories;

import com.rk.inventory_management_system.entities.Customer;
import com.rk.inventory_management_system.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByCustomer(Customer customer);
}
