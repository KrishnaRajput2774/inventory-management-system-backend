package com.rk.inventory_management_system.schedulers;

import com.rk.inventory_management_system.entities.Order;
import com.rk.inventory_management_system.entities.Product;
import com.rk.inventory_management_system.services.EmailService;
import com.rk.inventory_management_system.services.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class LowStockScheduler {

    private final StockService stockService;
    private final EmailService emailService;


    @Value("${admin.email}")
    private String adminEmail;


    @Scheduled(cron = "0 0 9 * * ?")
    public void sendDailyLowStockEmail() {
        log.info("Starting daily low stock email check");
        List<Product> lowStockProducts = stockService.getLowStockProducts();
        if (!lowStockProducts.isEmpty()) {
            log.info("Found {} products with low stock, sending daily alert email", lowStockProducts.size());
            emailService.sendLowStockAlertHtml(adminEmail, lowStockProducts);
        } else {
            log.info("No low stock products found for daily alert");
        }
    }

    // Order-specific low stock alert (triggered after order completion)
    public void checkAndSendLowStockAlertOrderSpecific(List<Product> products, Order completedOrder) {
        log.info("Checking order-specific low stock alert for Order ID: {}", completedOrder.getId());

        // Filter products that are now below their low stock threshold
        List<Product> lowStockProducts = products.stream()
                .filter(product -> product.getLowStockThreshold() > product.getStockQuantity())
                .collect(Collectors.toList());

        if (!lowStockProducts.isEmpty()) {
            log.info("Order #{} caused {} products to fall below low stock threshold, sending alert email",
                    completedOrder.getId(), lowStockProducts.size());
            emailService.sendOrderSpecificLowStockAlertHtml(adminEmail, lowStockProducts, completedOrder);
        } else {
            log.info("No products fell below low stock threshold after Order #{}", completedOrder.getId());
        }

        log.info("Order-specific low stock check completed for Order #{}", completedOrder.getId());
    }

}
