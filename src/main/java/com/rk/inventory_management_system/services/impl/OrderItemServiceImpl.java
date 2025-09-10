package com.rk.inventory_management_system.services.impl;

import com.rk.inventory_management_system.dtos.OrderItemDto;
import com.rk.inventory_management_system.dtos.ProductDto;
import com.rk.inventory_management_system.dtos.ResponseDtos.OrderItemResponseDto;
import com.rk.inventory_management_system.dtos.ResponseDtos.OrderResponseDto;
import com.rk.inventory_management_system.entities.Enums.OrderStatus;
import com.rk.inventory_management_system.entities.Enums.OrderType;
import com.rk.inventory_management_system.entities.Order;
import com.rk.inventory_management_system.entities.OrderItem;
import com.rk.inventory_management_system.entities.Product;
import com.rk.inventory_management_system.exceptions.InsufficientStockException;
import com.rk.inventory_management_system.exceptions.OrderItemManagementException;
import com.rk.inventory_management_system.exceptions.ResourceNotFoundException;
import com.rk.inventory_management_system.exceptions.StockManagementException;
import com.rk.inventory_management_system.repositories.OrderRepository;
import com.rk.inventory_management_system.repositories.ProductRepository;
import com.rk.inventory_management_system.schedulers.LowStockScheduler;
import com.rk.inventory_management_system.services.OrderItemService;
import com.rk.inventory_management_system.services.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderItemServiceImpl implements OrderItemService {

    private final ProductService productService;
    private final OrderRepository orderRepository;
    private final ModelMapper modelMapper;
    private final LowStockScheduler lowStockScheduler;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public OrderResponseDto addItemToOrder(Long orderId, OrderItemDto orderItemDto) {
        log.info("Adding item to order ID: {}", orderId);

        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }

        if (orderItemDto == null) {
            throw new IllegalArgumentException("Order item data cannot be null");
        }

        // Validate the order item
        validateOrderItemForAdd(orderItemDto);

        Order order = getOrderWithItems(orderId);

        // Check if order can be modified
        validateOrderCanBeModified(order);

        try {
            // Handle stock validation for outward orders
            if (OrderType.SALE.equals(order.getOrderType())) {
                validateStockAvailabilityForSingleItem(orderItemDto);
            }

            // Initialize order items list if null
            if (order.getOrderItems() == null) {
                order.setOrderItems(new ArrayList<>());
            }

            // Check if the product already exists in the order
            Long productId = orderItemDto.getProductDto().getProductId();
            Optional<OrderItem> existingOrderItem = order.getOrderItems()
                    .stream()
                    .filter(item -> item.getProduct() != null && item.getProduct().getId().equals(productId))
                    .findFirst();

            if (existingOrderItem.isPresent()) {
                // Product already exists in order - update quantity
                log.info("Product ID {} already exists in order {}. Updating quantity.", productId, orderId);
                updateExistingOrderItem(existingOrderItem.get(), orderItemDto, order);
            } else {
                // Product doesn't exist in order - create new order item
                log.info("Adding new product ID {} to order {}.", productId, orderId);
                OrderItem newOrderItem = createOrderItemForExistingOrder(order, orderItemDto);
                order.getOrderItems().add(newOrderItem);
            }

            // Recalculate total price
            calculateOrderTotal(order);
            order.setUpdatedAt(LocalDateTime.now());

            Order savedOrder = orderRepository.save(order);
            log.info("Successfully updated order ID: {}, new total: {}",
                    orderId, savedOrder.getTotalPrice());

            return buildOrderResponseDto(savedOrder);

        } catch (InsufficientStockException | ResourceNotFoundException | IllegalArgumentException e) {
            log.error("Business logic error adding item to order {}: {}", orderId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error adding item to order {}: {}", orderId, e.getMessage(), e);
            throw new OrderItemManagementException("Failed to add item to order", e);
        }
    }

    // Enhanced helper method to update existing order item
    private void updateExistingOrderItem(OrderItem existingOrderItem, OrderItemDto orderItemDto, Order order) {
        int newQuantityToAdd = orderItemDto.getQuantity();
        int currentQuantity = existingOrderItem.getQuantity();
        int newTotalQuantity = currentQuantity + newQuantityToAdd;

        log.info("Updating existing order item. Current quantity: {}, Adding: {}, New total: {}",
                currentQuantity, newQuantityToAdd, newTotalQuantity);

        Product product = existingOrderItem.getProduct();
        List<Product> productsForAlert = new ArrayList<>();

        if (OrderType.SALE.equals(order.getOrderType())) {
            // For sale orders, reduce stock and trigger alerts
            productService.reduceStockOfProduct(product.getId(), newQuantityToAdd);
            productsForAlert.add(product);

            // Handle quantitySold only if order is already COMPLETED
            if (OrderStatus.COMPLETED.equals(order.getOrderStatus())) {
                int currentQuantitySold = product.getQuantitySold() != null ? product.getQuantitySold() : 0;
                product.setQuantitySold(currentQuantitySold + newQuantityToAdd);
                log.info("Updated quantitySold for completed order. Product: {}, Added: {}, New total: {}",
                        product.getName(), newQuantityToAdd, product.getQuantitySold());
            }

        } else if (OrderType.PURCHASE.equals(order.getOrderType())) {
            // For purchase orders, increase stock
            productService.increaseStockOfProduct(product.getId(), newQuantityToAdd);
        }

        // Update the existing order item quantity
        existingOrderItem.setQuantity(newTotalQuantity);

        // Update price if provided in the DTO
        if (orderItemDto.getPriceAtOrderTime() != null) {
            existingOrderItem.setPriceAtOrderTime(orderItemDto.getPriceAtOrderTime());
        }

        // Trigger low stock alerts for sale orders
        if (!productsForAlert.isEmpty()) {
            lowStockScheduler.checkAndSendLowStockAlertOrderSpecific(productsForAlert, order);
        }

        log.info("Successfully updated order item ID: {} with new quantity: {}",
                existingOrderItem.getId(), newTotalQuantity);
    }

    @Override
    @Transactional
    public List<OrderItemDto> removeItemFromOrder(Long orderId, Long orderItemId, Integer quantityToRemove) {
        log.info("Removing {} units from order item ID: {} in order ID: {}",
                quantityToRemove, orderItemId, orderId);

        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }

        if (orderItemId == null) {
            throw new IllegalArgumentException("Order item ID cannot be null");
        }

        if (quantityToRemove == null || quantityToRemove <= 0) {
            throw new IllegalArgumentException("Quantity to remove must be greater than zero");
        }

        Order order = getOrderWithItems(orderId);

        // Check if order can be modified
        validateOrderCanBeModified(order);

        OrderItem orderItem = order.getOrderItems()
                .stream()
                .filter(item -> item.getId().equals(orderItemId))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Order item not found with ID: {} in order: {}", orderItemId, orderId);
                    return new ResourceNotFoundException("Order item not found with ID: " + orderItemId);
                });

        if (quantityToRemove > orderItem.getQuantity()) {
            throw new IllegalArgumentException(
                    String.format("Cannot remove %d units. Order item only has %d units.",
                            quantityToRemove, orderItem.getQuantity())
            );
        }

        try {
            Product product = orderItem.getProduct();

            // Handle stock restoration and quantitySold adjustment
            if (OrderType.SALE.equals(order.getOrderType())) {
                // Always restore stock for removed quantities in sale orders
                restoreStockForRemovedQuantity(product.getId(), quantityToRemove);

                // Handle quantitySold only if order is already COMPLETED
                if (OrderStatus.COMPLETED.equals(order.getOrderStatus())) {
                    int currentQuantitySold = product.getQuantitySold() != null ? product.getQuantitySold() : 0;
                    int newQuantitySold = Math.max(0, currentQuantitySold - quantityToRemove);
                    product.setQuantitySold(newQuantitySold);
                    productRepository.save(product);

                    log.info("Adjusted quantitySold for completed order. Product: {}, Removed: {}, New total: {}",
                            product.getName(), quantityToRemove, newQuantitySold);
                }
            } else if (OrderType.PURCHASE.equals(order.getOrderType())) {
                // For purchase orders, reduce stock when removing items
                if (product.getStockQuantity() >= quantityToRemove) {
                    productService.reduceStockOfProduct(product.getId(), quantityToRemove);
                } else {
                    log.warn("Cannot reduce stock for product {} below zero. Current stock: {}, Trying to remove: {}",
                            product.getName(), product.getStockQuantity(), quantityToRemove);
                }
            }

            if (quantityToRemove.equals(orderItem.getQuantity())) {
                // Remove the entire item
                order.getOrderItems().remove(orderItem);
                log.info("Completely removed order item ID: {} from order ID: {}", orderItemId, orderId);
            } else {
                // Reduce quantity
                orderItem.setQuantity(orderItem.getQuantity() - quantityToRemove);
                log.info("Reduced order item ID: {} quantity by {} units", orderItemId, quantityToRemove);
            }

            // Recalculate total price
            calculateOrderTotal(order);
            order.setUpdatedAt(LocalDateTime.now());

            Order savedOrder = orderRepository.save(order);

            log.info("Successfully updated order ID: {}, new total: {}",
                    orderId, savedOrder.getTotalPrice());

            return savedOrder.getOrderItems()
                    .stream()
                    .map(item -> modelMapper.map(item, OrderItemDto.class))
                    .toList();

        } catch (Exception e) {
            log.error("Failed to remove item from order {}: {}", orderId, e.getMessage(), e);
            throw new OrderItemManagementException("Failed to remove item from order", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getAllItemsByOrder(Long orderId) {
        log.info("Fetching all items for order ID: {}", orderId);

        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }

        Order order = getOrderWithItems(orderId);

        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            log.info("No items found for order ID: {}", orderId);
            return buildEmptyOrderResponseDto(order);
        }

        log.info("Retrieved {} items for order ID: {}", order.getOrderItems().size(), orderId);
        return buildOrderResponseDto(order);
    }

    // --- Helper Methods ---

    private Order getOrderWithItems(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Order not found with ID: {}", orderId);
                    return new ResourceNotFoundException("Order not found with ID: " + orderId);
                });

        // Force lazy loading of order items and their products
        if (order.getOrderItems() != null) {
            order.getOrderItems().forEach(item -> {
                if (item.getProduct() != null) {
                    // Access product properties to ensure it's loaded
                    item.getProduct().getName();
                } else {
                    log.warn("Order item {} has null product", item.getId());
                }
            });
        }

        return order;
    }

    private void validateOrderItemForAdd(OrderItemDto orderItemDto) {
        if (orderItemDto.getProductDto().getProductId() == null) {
            throw new IllegalArgumentException("Product ID is required");
        }

        if (orderItemDto.getQuantity() == null || orderItemDto.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (orderItemDto.getPriceAtOrderTime() != null && orderItemDto.getPriceAtOrderTime() < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
    }

    private void validateOrderCanBeModified(Order order) {
        if (OrderStatus.COMPLETED.equals(order.getOrderStatus())) {
            log.warn("Attempting to modify completed order ID: {}", order.getId());
            throw new IllegalStateException("Cannot modify completed order. Use order status management for completed orders.");
        }

        if (OrderStatus.CANCELLED.equals(order.getOrderStatus())) {
            throw new IllegalStateException("Cannot modify cancelled order");
        }
    }

    private void validateStockAvailabilityForSingleItem(OrderItemDto orderItemDto) {
        Long productId = orderItemDto.getProductDto().getProductId();
        int quantityRequested = orderItemDto.getQuantity();

        // Fetch managed entity directly from repository
        Product product = productService.getProductByIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        int totalProductStock = productService.findProductByNameAndBrandName(product.getName(), product.getBrandName())
                .stream()
                .mapToInt(Product::getStockQuantity)
                .sum();

        if (totalProductStock < quantityRequested) {
            throw new InsufficientStockException(
                    String.format("Insufficient stock for product: %s - Requested: %d, Available: %d",
                            product.getName(), quantityRequested, totalProductStock)
            );
        }
    }

    private OrderItem createOrderItemForExistingOrder(Order order, OrderItemDto orderItemDto) {
        Long productId = orderItemDto.getProductDto().getProductId();
        int quantity = orderItemDto.getQuantity();

        // Fetch the managed entity directly from repository
        Product product = productService.getProductByIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        if (OrderType.SALE.equals(order.getOrderType())) {
            // For sale orders, reduce stock and trigger alerts
            List<ProductDto> updatedProductDtos = productService.reduceStockOfProduct(productId, quantity);
            if (updatedProductDtos == null || updatedProductDtos.isEmpty()) {
                throw new StockManagementException("Failed to reduce stock for product ID: " + productId);
            }

            // Trigger low stock alert
            lowStockScheduler.checkAndSendLowStockAlertOrderSpecific(List.of(product), order);

            // Handle quantitySold only if order is already COMPLETED
            if (OrderStatus.COMPLETED.equals(order.getOrderStatus())) {
                int currentQuantitySold = product.getQuantitySold() != null ? product.getQuantitySold() : 0;
                product.setQuantitySold(currentQuantitySold + quantity);
                productRepository.save(product);

                log.info("Updated quantitySold for completed order. Product: {}, Added: {}, New total: {}",
                        product.getName(), quantity, product.getQuantitySold());
            }

            return createOrderItem(order, product, orderItemDto);

        } else if (OrderType.PURCHASE.equals(order.getOrderType())) {
            // For purchase orders, increase stock
            ProductDto updatedProductDto = productService.increaseStockOfProduct(productId, quantity);
            if (updatedProductDto == null) {
                throw new StockManagementException("Failed to increase stock for product ID: " + productId);
            }
            return createOrderItem(order, product, orderItemDto);

        } else {
            throw new IllegalArgumentException("Unsupported order type: " + order.getOrderType());
        }
    }

    private OrderItem createOrderItem(Order order, Product product, OrderItemDto orderItemDto) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null when creating order item");
        }

        if (product.getId() == null) {
            throw new IllegalArgumentException("Product ID cannot be null when creating order item");
        }

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(orderItemDto.getQuantity());

        // Use price from DTO if provided, otherwise use product price
        Double priceToUse = orderItemDto.getPriceAtOrderTime() != null ?
                orderItemDto.getPriceAtOrderTime() : product.getSellingPrice();
        orderItem.setPriceAtOrderTime(priceToUse);

        log.debug("Created order item with product ID: {} for order ID: {}",
                product.getId(), order.getId());

        return orderItem;
    }

    private void restoreStockForRemovedQuantity(Long productId, int quantityToRestore) {
        try {
            productService.increaseStockOfProduct(productId, quantityToRestore);
            log.debug("Restored {} units for product ID: {}", quantityToRestore, productId);
        } catch (Exception e) {
            log.error("Failed to restore stock for product ID {}: {}", productId, e.getMessage());
            throw new StockManagementException("Failed to restore stock for removed item", e);
        }
    }

    private void calculateOrderTotal(Order order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            order.setTotalPrice(0.0);
            return;
        }

        double total = order.getOrderItems()
                .stream()
                .mapToDouble(item -> {
                    double price = item.getPriceAtOrderTime() != null ?
                            item.getPriceAtOrderTime() :
                            (item.getProduct() != null ? item.getProduct().getSellingPrice() : 0.0);
                    return price * item.getQuantity();
                })
                .sum();

        order.setTotalPrice(total);
    }

    private OrderResponseDto buildOrderResponseDto(Order savedOrder) {
        log.info("Building Order Response, Order with Id: " + savedOrder.getId());

        List<OrderItemResponseDto> items = savedOrder.getOrderItems()
                .stream()
                .map(item -> {
                    if (item.getProduct() == null) {
                        log.warn("Order item {} has null product, skipping", item.getId());
                        return null;
                    }

                    return OrderItemResponseDto.builder()
                            .id(item.getId())
                            .productId(item.getProduct().getId())
                            .productName(item.getProduct().getName())
                            .price(item.getPriceAtOrderTime() != null ?
                                    item.getPriceAtOrderTime() : item.getProduct().getSellingPrice())
                            .brand(item.getProduct().getBrandName())
                            .quantity(item.getQuantity())
                            .totalPrice(item.getQuantity() * (item.getPriceAtOrderTime() != null ?
                                    item.getPriceAtOrderTime() : item.getProduct().getSellingPrice()))
                            .build();
                })
                .filter(item -> item != null)
                .toList();

        Double totalOrderPrice = items.stream()
                .mapToDouble(OrderItemResponseDto::getTotalPrice)
                .sum();

        return OrderResponseDto.builder()
                .orderId(savedOrder.getId())
                .customerId(savedOrder.getCustomer() != null ? savedOrder.getCustomer().getId() : null)
                .customerName(savedOrder.getCustomer() != null ? savedOrder.getCustomer().getName() : null)
                .totalPrice(totalOrderPrice)
                .items(items)
                .build();
    }

    private OrderResponseDto buildEmptyOrderResponseDto(Order order) {
        return OrderResponseDto.builder()
                .orderId(order.getId())
                .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
                .customerName(order.getCustomer() != null ? order.getCustomer().getName() : null)
                .totalPrice(0.0)
                .items(new ArrayList<>())
                .build();
    }
}