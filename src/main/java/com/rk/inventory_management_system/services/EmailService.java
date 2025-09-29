package com.rk.inventory_management_system.services;

import com.rk.inventory_management_system.entities.Order;
import com.rk.inventory_management_system.entities.Product;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    // Updated database-specific low stock alert with better mobile responsiveness
    public void sendLowStockAlertHtml(String to, List<Product> lowStockProducts) {
        log.info("Sending HTML Low stock email to: {}", to);

        String subject = "⚠️ URGENT: Daily Low Stock Alert - " + lowStockProducts.size() + " Products Need Attention";

        // Calculate statistics
        int criticalStock = 0;
        int lowStock = 0;
        double totalValue = 0.0;

        for (Product p : lowStockProducts) {
            if (p.getStockQuantity() <= 5) {
                criticalStock++;
            } else {
                lowStock++;
            }
            totalValue += (p.getActualPrice() * p.getStockQuantity());
        }

        // Sort products by stock quantity (lowest first)
        lowStockProducts.sort((p1, p2) -> Integer.compare(p1.getStockQuantity(), p2.getStockQuantity()));

        String htmlContent = buildLowStockEmailTemplate(lowStockProducts, criticalStock, lowStock, totalValue, "Daily Inventory Report", null);

        sendEmail(to, subject, htmlContent);
    }

    // New order-specific low stock alert
    public void sendOrderSpecificLowStockAlertHtml(String to, List<Product> lowStockProducts, Order completedOrder) {
        log.info("Sending Order-Specific HTML Low stock email to: {}", to);

        String subject = "⚠️ ORDER ALERT: Low Stock After Order #" + completedOrder.getId() + " - " + lowStockProducts.size() + " Products Affected";

        // Calculate statistics
        int criticalStock = 0;
        int lowStock = 0;
        double totalValue = 0.0;

        for (Product p : lowStockProducts) {
            if (p.getStockQuantity() <= 5) {
                criticalStock++;
            } else {
                lowStock++;
            }
            totalValue += (p.getActualPrice() * p.getStockQuantity());
        }

        // Sort products by stock quantity (lowest first)
        lowStockProducts.sort((p1, p2) -> Integer.compare(p1.getStockQuantity(), p2.getStockQuantity()));

        String htmlContent = buildLowStockEmailTemplate(lowStockProducts, criticalStock, lowStock, totalValue, "Post-Order Inventory Alert", completedOrder);

        sendEmail(to, subject, htmlContent);
    }

    private String buildLowStockEmailTemplate(List<Product> lowStockProducts, int criticalStock, int lowStock,
                                              double totalValue, String alertType, Order order) {
        StringBuilder htmlContent = new StringBuilder();

        htmlContent.append("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Low Stock Alert</title>
                    <style>
                        body { 
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
                            margin: 0; 
                            padding: 20px; 
                            background-color: #f8f9fa; 
                            color: #333;
                            line-height: 1.6;
                            -webkit-text-size-adjust: 100%;
                        }
                        .container { 
                            max-width: 800px; 
                            margin: 0 auto; 
                            background-color: white; 
                            border-radius: 10px; 
                            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
                            overflow: hidden;
                        }
                        .header { 
                            background: linear-gradient(135deg, #dc3545, #c82333); 
                            color: white; 
                            padding: 30px; 
                            text-align: center;
                        }
                        .header h1 { 
                            margin: 0; 
                            font-size: 28px; 
                            font-weight: 600;
                        }
                        .header p { 
                            margin: 10px 0 0 0; 
                            font-size: 16px; 
                            opacity: 0.9;
                        }
                        .content { 
                            padding: 30px; 
                        }
                        
                        /* Order Information Section */
                        .order-info { 
                            background: linear-gradient(135deg, #e8f5e8, #d4edda); 
                            padding: 20px; 
                            margin: 20px 0; 
                            border-radius: 8px; 
                            border-left: 5px solid #28a745;
                        }
                        .order-info h3 { 
                            margin-top: 0; 
                            color: #155724; 
                            font-size: 20px;
                        }
                        .order-details {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
                            gap: 15px;
                            margin-top: 15px;
                        }
                        .order-detail-item {
                            background: white;
                            padding: 10px;
                            border-radius: 4px;
                            border: 1px solid #c3e6cb;
                        }
                        .order-detail-label {
                            font-size: 11px;
                            color: #155724;
                            text-transform: uppercase;
                            font-weight: bold;
                        }
                        .order-detail-value {
                            font-size: 14px;
                            color: #333;
                            margin-top: 2px;
                        }
                        
                        .summary { 
                            background: linear-gradient(135deg, #e3f2fd, #bbdefb); 
                            padding: 20px; 
                            margin: 20px 0; 
                            border-radius: 8px; 
                            border-left: 5px solid #2196f3;
                        }
                        .summary h3 { 
                            margin-top: 0; 
                            color: #1976d2; 
                            font-size: 20px;
                        }
                        .stats-grid { 
                            display: grid; 
                            grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); 
                            gap: 15px; 
                            margin: 15px 0;
                        }
                        .stat-card { 
                            background: white; 
                            padding: 15px; 
                            border-radius: 6px; 
                            border: 1px solid #ddd;
                            text-align: center;
                        }
                        .stat-number { 
                            font-size: 24px; 
                            font-weight: bold; 
                            margin-bottom: 5px;
                        }
                        .stat-label { 
                            font-size: 12px; 
                            color: #666; 
                            text-transform: uppercase;
                        }
                        .critical-number { color: #dc3545; }
                        .low-number { color: #ffc107; }
                        .value-number { color: #28a745; }
                
                        .products-section { 
                            margin: 30px 0; 
                        }
                        .products-section h3 { 
                            color: #333; 
                            border-bottom: 2px solid #dee2e6; 
                            padding-bottom: 10px;
                        }

                        /* Mobile-first responsive table */
                        .product-table { 
                            width: 100%; 
                            border-collapse: collapse; 
                            margin: 20px 0;
                            background: white;
                            border-radius: 8px;
                            overflow: hidden;
                            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        }
                        .product-table th { 
                            background: linear-gradient(135deg, #495057, #343a40); 
                            color: white; 
                            padding: 15px 8px; 
                            text-align: left; 
                            font-weight: 600;
                            font-size: 12px;
                            text-transform: uppercase;
                        }
                        .product-table td { 
                            padding: 12px 8px; 
                            border-bottom: 1px solid #dee2e6;
                            vertical-align: top;
                            word-wrap: break-word;
                            overflow-wrap: break-word;
                        }
                        .product-table tr:hover { 
                            background-color: #f8f9fa; 
                        }

                        /* Mobile Card Layout - Hidden by default */
                        .mobile-card {
                            display: none;
                            background: white;
                            margin: 15px 0;
                            border-radius: 8px;
                            overflow: hidden;
                            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        }
                        .mobile-card-header {
                            padding: 15px;
                            border-left: 4px solid #2196f3;
                        }
                        .mobile-card-body {
                            padding: 0 15px 15px 15px;
                        }
                        .mobile-card-row {
                            display: flex;
                            justify-content: space-between;
                            padding: 8px 0;
                            border-bottom: 1px solid #eee;
                        }
                        .mobile-card-row:last-child {
                            border-bottom: none;
                        }
                        .mobile-card-label {
                            font-weight: 600;
                            color: #666;
                            font-size: 12px;
                            text-transform: uppercase;
                        }
                        .mobile-card-value {
                            text-align: right;
                            color: #333;
                        }
                
                        .priority-critical { 
                            background-color: #f8d7da !important; 
                            border-left: 4px solid #dc3545 !important;
                        }
                        .priority-low { 
                            background-color: #fff3cd !important; 
                            border-left: 4px solid #ffc107 !important;
                        }
                
                        .priority-badge { 
                            padding: 4px 8px; 
                            border-radius: 12px; 
                            font-size: 11px; 
                            font-weight: bold; 
                            text-transform: uppercase;
                            white-space: nowrap;
                        }
                        .badge-critical { 
                            background-color: #dc3545; 
                            color: white; 
                        }
                        .badge-low { 
                            background-color: #ffc107; 
                            color: #333; 
                        }
                
                        .product-name { 
                            font-weight: 600; 
                            color: #495057;
                            font-size: 14px;
                        }
                        .brand-name { 
                            font-size: 12px; 
                            color: #6c757d; 
                            margin-top: 2px;
                        }
                
                        .stock-info { 
                            text-align: center;
                        }
                        .current-stock { 
                            font-size: 16px; 
                            font-weight: bold;
                        }
                        .threshold { 
                            font-size: 11px; 
                            color: #666; 
                            margin-top: 2px;
                        }
                
                        .price-info { 
                            text-align: right;
                        }
                        .unit-price { 
                            font-weight: 600;
                            font-size: 14px;
                        }
                        .total-value { 
                            font-size: 11px; 
                            color: #28a745; 
                            margin-top: 2px;
                        }
                        .discount-price { 
                            font-size: 11px; 
                            color: #dc3545; 
                            text-decoration: line-through;
                        }
                
                        .reorder-info { 
                            text-align: center; 
                            background-color: #e7f3ff; 
                            padding: 8px; 
                            border-radius: 4px;
                        }
                        .reorder-qty { 
                            font-weight: bold; 
                            color: #0066cc;
                            font-size: 13px;
                        }
                        .reorder-cost { 
                            font-size: 10px; 
                            color: #666; 
                            margin-top: 2px;
                        }
                
                        .actions-section { 
                            background: linear-gradient(135deg, #fff3e0, #ffe0b2); 
                            padding: 25px; 
                            margin: 30px 0; 
                            border-radius: 8px; 
                            border-left: 5px solid #ff9800;
                        }
                        .actions-section h3 { 
                            color: #f57c00; 
                            margin-top: 0;
                        }
                        .action-list { 
                            list-style: none; 
                            padding: 0;
                        }
                        .action-list li { 
                            padding: 8px 0; 
                            padding-left: 25px; 
                            position: relative;
                        }
                        .action-list li::before { 
                            content: "→"; 
                            position: absolute; 
                            left: 0; 
                            color: #ff9800; 
                            font-weight: bold;
                        }
                
                        .footer { 
                            background-color: #f8f9fa; 
                            padding: 20px; 
                            text-align: center; 
                            border-top: 1px solid #dee2e6; 
                            font-size: 12px; 
                            color: #6c757d;
                        }

                        /* Mobile Styles */
                        @media (max-width: 768px) {
                            body { 
                                padding: 10px; 
                                font-size: 14px;
                            }
                            .container { 
                                margin: 0; 
                                border-radius: 0;
                            }
                            .content { 
                                padding: 15px; 
                            }
                            .header { 
                                padding: 20px 15px; 
                            }
                            .header h1 { 
                                font-size: 22px; 
                            }
                            .stats-grid { 
                                grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
                                gap: 10px;
                            }
                            .stat-card {
                                padding: 10px;
                            }
                            .stat-number {
                                font-size: 18px;
                            }
                            .order-details {
                                grid-template-columns: 1fr;
                                gap: 10px;
                            }
                            
                            /* Hide table on mobile */
                            .product-table {
                                display: none;
                            }
                            
                            /* Show mobile cards */
                            .mobile-card {
                                display: block;
                            }
                        }

                        /* Very small screens */
                        @media (max-width: 480px) {
                            .stats-grid { 
                                grid-template-columns: 1fr 1fr; 
                            }
                            .header h1 {
                                font-size: 18px;
                            }
                            .header p {
                                font-size: 14px;
                            }
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                """);

        // Header
        htmlContent.append("<div class='header'>")
                .append("<h1>🚨 LOW STOCK ALERT</h1>")
                .append("<p>").append(alertType).append(" - ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a"))).append("</p>")
                .append("</div>");

        // Content
        htmlContent.append("<div class='content'>");

        // Order Information Section (only for order-specific alerts)
        if (order != null) {
            htmlContent.append("<div class='order-info'>")
                    .append("<h3>📋 Order Information</h3>")
                    .append("<div class='order-details'>")
                    .append("<div class='order-detail-item'>")
                    .append("<div class='order-detail-label'>Order ID</div>")
                    .append("<div class='order-detail-value'>#").append(order.getId()).append("</div>")
                    .append("</div>")
                    .append("<div class='order-detail-item'>")
                    .append("<div class='order-detail-label'>Total Amount</div>")
                    .append("<div class='order-detail-value'>Rs.").append(String.format("%.2f", order.getTotalPrice())).append("</div>")
                    .append("</div>")
                    .append("<div class='order-detail-item'>")
                    .append("<div class='order-detail-label'>Order Date</div>")
                    .append("<div class='order-detail-value'>").append(order.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))).append("</div>")
                    .append("</div>");

            if (order.getCustomer() != null) {
                htmlContent.append("<div class='order-detail-item'>")
                        .append("<div class='order-detail-label'>Customer</div>")
                        .append("<div class='order-detail-value'>").append(order.getCustomer().getName()).append("</div>")
                        .append("</div>");
            }

            htmlContent.append("</div>")
                    .append("<p style='margin-top: 15px; color: #155724;'><strong>This alert was triggered after processing the above order.</strong></p>")
                    .append("</div>");
        }

        // Summary Section
        htmlContent.append("<div class='summary'>")
                .append("<h3>📊 Executive Summary</h3>")
                .append("<div class='stats-grid'>")
                .append("<div class='stat-card'>")
                .append("<div class='stat-number critical-number'>").append(criticalStock).append("</div>")
                .append("<div class='stat-label'>Critical Stock</div>")
                .append("</div>")
                .append("<div class='stat-card'>")
                .append("<div class='stat-number low-number'>").append(lowStock).append("</div>")
                .append("<div class='stat-label'>Low Stock</div>")
                .append("</div>")
                .append("<div class='stat-card'>")
                .append("<div class='stat-number value-number'>Rs.").append(String.format("%.0f", totalValue)).append("</div>")
                .append("<div class='stat-label'>Value at Risk</div>")
                .append("</div>")
                .append("</div>")
                .append("</div>");

        // Products Section
        htmlContent.append("<div class='products-section'>")
                .append("<h3>📦 Product Details</h3>");

        // Desktop Table
        htmlContent.append("<table class='product-table'>")
                .append("<thead>")
                .append("<tr>")
                .append("<th style='width: 10%;'>Priority</th>")
                .append("<th style='width: 25%;'>Product</th>")
                .append("<th style='width: 15%;'>Stock</th>")
                .append("<th style='width: 15%;'>Price</th>")
                .append("<th style='width: 20%;'>Category</th>")
                .append("<th style='width: 15%;'>Reorder</th>")
                .append("</tr>")
                .append("</thead>")
                .append("<tbody>");

        // Product rows for desktop table and mobile cards
        for (Product p : lowStockProducts) {
            boolean isCritical = p.getStockQuantity() <= 5;
            String priorityClass = isCritical ? "priority-critical" : "priority-low";
            String badgeClass = isCritical ? "badge-critical" : "badge-low";
            String priorityText = isCritical ? "Critical" : "Low";

            int suggestedReorder = Math.max(p.getLowStockThreshold() * 2, 10);
            double reorderCost = p.getActualPrice() * suggestedReorder;

            // Desktop table row
            htmlContent.append("<tr class='").append(priorityClass).append("'>")
                    .append("<td><span class='priority-badge ").append(badgeClass).append("'>").append(priorityText).append("</span></td>")
                    .append("<td>")
                    .append("<div class='product-name'>").append(p.getName()).append("</div>")
                    .append("<div class='brand-name'>").append(p.getBrandName() != null ? p.getBrandName() : "No Brand").append("</div>")
                    .append("</td>")
                    .append("<td class='stock-info'>")
                    .append("<div class='current-stock ").append(isCritical ? "critical-number" : "low-number").append("'>").append(p.getStockQuantity()).append("</div>")
                    .append("<div class='threshold'>Limit: ").append(p.getLowStockThreshold()).append("</div>")
                    .append("</td>")
                    .append("<td class='price-info'>")
                    .append("<div class='unit-price'>Rs.").append(String.format("%.0f", p.getActualPrice())).append("</div>");

            if (p.getDiscount() != null && p.getDiscount() > 0) {
                double discountedPrice = p.getSellingPrice() * (1 - p.getDiscount() / 100);
                htmlContent.append("<div class='discount-price'>Rs.").append(String.format("%.0f", discountedPrice)).append("</div>");
            }

            htmlContent.append("<div class='total-value'>Value: Rs.").append(String.format("%.0f", p.getActualPrice() * p.getStockQuantity())).append("</div>")
                    .append("</td>")
                    .append("<td>")
                    .append("<div style='font-size: 12px;'>").append(p.getCategory() != null ? p.getCategory().getName() : "Uncategorized").append("</div>")
                    .append("<div style='font-size: 10px; color: #666; margin-top: 2px;'>").append(p.getSupplier() != null ? p.getSupplier().getName() : "No Supplier").append("</div>")
                    .append("</td>")
                    .append("<td>")
                    .append("<div class='reorder-info'>")
                    .append("<div class='reorder-qty'>").append(suggestedReorder).append(" units</div>")
                    .append("<div class='reorder-cost'>~Rs.").append(String.format("%.0f", reorderCost)).append("</div>")
                    .append("</div>")
                    .append("</td>")
                    .append("</tr>");

            // Mobile card
            htmlContent.append("<div class='mobile-card ").append(priorityClass).append("'>")
                    .append("<div class='mobile-card-header'>")
                    .append("<div style='display: flex; justify-content: space-between; align-items: center;'>")
                    .append("<div>")
                    .append("<div class='product-name'>").append(p.getName()).append("</div>")
                    .append("<div class='brand-name'>").append(p.getBrandName() != null ? p.getBrandName() : "No Brand").append("</div>")
                    .append("</div>")
                    .append("<span class='priority-badge ").append(badgeClass).append("'>").append(priorityText).append("</span>")
                    .append("</div>")
                    .append("</div>")
                    .append("<div class='mobile-card-body'>")
                    .append("<div class='mobile-card-row'>")
                    .append("<span class='mobile-card-label'>Current Stock</span>")
                    .append("<span class='mobile-card-value current-stock ").append(isCritical ? "critical-number" : "low-number").append("'>").append(p.getStockQuantity()).append(" units</span>")
                    .append("</div>")
                    .append("<div class='mobile-card-row'>")
                    .append("<span class='mobile-card-label'>Threshold</span>")
                    .append("<span class='mobile-card-value'>").append(p.getLowStockThreshold()).append(" units</span>")
                    .append("</div>")
                    .append("<div class='mobile-card-row'>")
                    .append("<span class='mobile-card-label'>Unit Price</span>")
                    .append("<span class='mobile-card-value unit-price'>Rs.").append(String.format("%.0f", p.getActualPrice())).append("</span>")
                    .append("</div>")
                    .append("<div class='mobile-card-row'>")
                    .append("<span class='mobile-card-label'>Stock Value</span>")
                    .append("<span class='mobile-card-value value-number'>Rs.").append(String.format("%.0f", p.getSellingPrice() * p.getStockQuantity())).append("</span>")
                    .append("</div>")
                    .append("<div class='mobile-card-row'>")
                    .append("<span class='mobile-card-label'>Category</span>")
                    .append("<span class='mobile-card-value'>").append(p.getCategory() != null ? p.getCategory().getName() : "Uncategorized").append("</span>")
                    .append("</div>")
                    .append("<div class='mobile-card-row'>")
                    .append("<span class='mobile-card-label'>Reorder Qty</span>")
                    .append("<span class='mobile-card-value reorder-qty'>").append(suggestedReorder).append(" units (~Rs.").append(String.format("%.0f", reorderCost)).append(")</span>")
                    .append("</div>")
                    .append("</div>")
                    .append("</div>");
        }

        htmlContent.append("</tbody></table></div>");

        // Actions Section
        String actionsTitle = order != null ? "⚡ Post-Order Actions Required" : "⚡ Immediate Action Required";
        htmlContent.append("<div class='actions-section'>")
                .append("<h3>").append(actionsTitle).append("</h3>")
                .append("<ul class='action-list'>")
                .append("<li><strong>Prioritize Critical Items:</strong> Focus on products with ≤5 units in stock</li>");

        if (order != null) {
            htmlContent.append("<li><strong>Review Order Impact:</strong> Analyze how Order #").append(order.getId()).append(" affected current inventory levels</li>");
        }

        htmlContent.append("<li><strong>Contact Suppliers:</strong> Initiate emergency restocking for critical items</li>")
                .append("<li><strong>Update Procurement:</strong> Place orders based on suggested quantities above</li>")
                .append("<li><strong>Monitor Closely:</strong> Check stock levels more frequently for affected items</li>")
                .append("<li><strong>Adjust Thresholds:</strong> Review and modify alert thresholds if needed</li>")
                .append("</ul>")
                .append("</div>");

        htmlContent.append("</div>"); // Close content

        // Footer
        String footerText = order != null ? "Post-Order Inventory Alert" : "Daily Inventory Alert";
        htmlContent.append("<div class='footer'>")
                .append("<p><strong>Inventory Management System</strong> | ").append(footerText).append("</p>")
                .append("<p>Generated on ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm:ss a"))).append("</p>")
                .append("<p>This is an automated message. For questions, contact your system administrator.</p>")
                .append("</div>");

        htmlContent.append("</div></body></html>"); // Close container and HTML

        return htmlContent.toString();
    }

    private void sendEmail(String to, String subject, String htmlContent) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);
            log.info("HTML Low stock alert email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send HTML low stock alert email to: {}. Error: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send HTML low stock alert email", e);
        }
    }
}