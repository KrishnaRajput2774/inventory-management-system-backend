package com.rk.inventory_management_system;

import com.rk.inventory_management_system.entities.Product;
import com.rk.inventory_management_system.repositories.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@SpringBootTest
class InventoryManagementSystemApplicationTests {

	@Test
	void contextLoads() {
	}

	@Autowired
	private ProductRepository productRepository;

	@Test
	@Transactional
	public void checkFindByNameContainingIgnoreCase() {
		List<Product> list = productRepository.findByNameContainingIgnoreCase("trimax");
		list.stream().forEach(product ->log.info("Product name: {}",product.getName()));
	}

	@Test
	@Transactional
	public void testFindByIdWithLock() {
		Product product = productRepository.findByIdWithLock(1L)
				.orElseThrow(() -> new RuntimeException("Product not found"));

		System.out.println("Product: " + product.getName());
	}

}
