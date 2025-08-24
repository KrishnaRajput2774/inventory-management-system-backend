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
import com.rk.inventory_management_system.repositories.OrderItemRepository;
import com.rk.inventory_management_system.repositories.OrderRepository;
import com.rk.inventory_management_system.services.OrderItemService;
import com.rk.inventory_management_system.services.OrderService;
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

    private final OrderService orderService;
    private final OrderItemRepository orderItemRepository;
    private final ProductService productService;
    private final OrderRepository orderRepository;
    private final ModelMapper modelMapper;

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
                updateExistingOrderItem(existingOrderItem.get(), orderItemDto, order.getOrderType());
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
    private void updateExistingOrderItem(OrderItem existingOrderItem, OrderItemDto orderItemDto, OrderType orderType) {
        int newQuantityToAdd = orderItemDto.getQuantity();
        int currentQuantity = existingOrderItem.getQuantity();
        int newTotalQuantity = currentQuantity + newQuantityToAdd;

        log.info("Updating existing order item. Current quantity: {}, Adding: {}, New total: {}",
                currentQuantity, newQuantityToAdd, newTotalQuantity);

        if (OrderType.SALE.equals(orderType)) {
            // For outward orders, reduce stock
            productService.reduceStockOfProduct(existingOrderItem.getProduct().getId(), newQuantityToAdd);
        } else if (OrderType.PURCHASE.equals(orderType)) {
            // For inward orders, increase stock
            productService.increaseStockOfProduct(existingOrderItem.getProduct().getId(), newQuantityToAdd);
        }

        // Update the existing order item quantity
        existingOrderItem.setQuantity(newTotalQuantity);

        // Update price if provided in the DTO (optional - you might want to keep original price)
        if (orderItemDto.getPriceAtOrderTime() != null) {
            existingOrderItem.setPriceAtOrderTime(orderItemDto.getPriceAtOrderTime());
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

        // Fix the logic - compare with orderItem quantity, not total stock
        if (quantityToRemove > orderItem.getQuantity()) {
            throw new IllegalArgumentException(
                    String.format("Cannot remove %d units. Order item only has %d units.",
                            quantityToRemove, orderItem.getQuantity())
            );
        }

        try {
            // Handle stock restoration for outward orders
            if (OrderType.SALE.equals(order.getOrderType())) {
                restoreStockForRemovedQuantity(orderItem.getProduct().getId(), quantityToRemove);
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
            return OrderResponseDto.builder().build();
        }

        log.info("Retrieved {} items for order ID: {}", order.getOrderItems().size(), orderId);
        return buildOrderResponseDto(order);
    }

    // --- Helper Methods ---

    // Enhanced method to fetch order with properly loaded relationships
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
            throw new IllegalStateException("Cannot modify completed order");
        }

        if (OrderStatus.CANCELLED.equals(order.getOrderStatus())) {
            throw new IllegalStateException("Cannot modify cancelled order");
        }
    }

    // ✅ FIXED: Now uses managed entity instead of DTO->Entity mapping
    private void validateStockAvailabilityForSingleItem(OrderItemDto orderItemDto) {
        Long productId = orderItemDto.getProductDto().getProductId();
        int quantityRequested = orderItemDto.getQuantity();

        // ✅ FIXED: Fetch managed entity directly from repository
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

    // ✅ FIXED: Enhanced createOrderItemForExistingOrder method with proper entity management
    private OrderItem createOrderItemForExistingOrder(Order order, OrderItemDto orderItemDto) {
        Long productId = orderItemDto.getProductDto().getProductId();
        int quantity = orderItemDto.getQuantity();

        // ✅ FIXED: Fetch the managed entity directly from repository instead of mapping DTO
        Product product = productService.getProductByIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));


        if (OrderType.SALE.equals(order.getOrderType())) {
            // For outward orders, reduce stock
            List<ProductDto> updatedProductDtos = productService.reduceStockOfProduct(productId, quantity);
            if (updatedProductDtos == null || updatedProductDtos.isEmpty()) {
                throw new StockManagementException("Failed to reduce stock for product ID: " + productId);
            }
            return createOrderItem(order, product, orderItemDto);

        } else if (OrderType.PURCHASE.equals(order.getOrderType())) {
            // For inward orders, increase stock
            ProductDto updatedProductDto = productService.increaseStockOfProduct(productId, quantity);
            if (updatedProductDto == null) {
                throw new StockManagementException("Failed to increase stock for product ID: " + productId);
            }
            return createOrderItem(order, product, orderItemDto);

        } else {
            throw new IllegalArgumentException("Unsupported order type: " + order.getOrderType());
        }
    }

    // Enhanced createOrderItem method with null checks
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
                orderItemDto.getPriceAtOrderTime() : product.getPrice();
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
                            (item.getProduct() != null ? item.getProduct().getPrice() : 0.0);
                    return price * item.getQuantity();
                })
                .sum();

        order.setTotalPrice(total);
    }

    // Fixed buildOrderResponseDto method with null product handling
    OrderResponseDto buildOrderResponseDto(Order savedOrder) {
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
                            .price(item.getProduct().getPrice())
                            .brand(item.getProduct().getBrandName())
                            .quantity(item.getQuantity())
                            .totalPrice(item.getQuantity() * item.getProduct().getPrice())
                            .build();
                })
                .filter(item -> item != null) // Remove null items
                .toList();

        Double totalOrderPrice = items.stream()
                .mapToDouble(OrderItemResponseDto::getTotalPrice)
                .sum();

        return OrderResponseDto.builder()
                .orderId(savedOrder.getId())
                .customerId(savedOrder.getCustomer().getId())
                .customerName(savedOrder.getCustomer().getName())
                .totalPrice(totalOrderPrice)
                .items(items)
                .build();
    }

//    @Override
//    public OrderItem getOrderItemById(Long orderItemId) {
//        return orderItemRepository.findById(orderItemId)
//                .orElseThrow(()->
//                        new ResourceNotFoundException("OrderItem not Found with id: "+orderItemId)
//                );
//    }
}