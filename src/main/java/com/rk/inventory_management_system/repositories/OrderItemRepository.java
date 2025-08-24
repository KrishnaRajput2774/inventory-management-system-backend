package com.rk.inventory_management_system.repositories;

import com.rk.inventory_management_system.entities.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
