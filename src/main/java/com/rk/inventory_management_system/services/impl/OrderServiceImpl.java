package com.rk.inventory_management_system.services.impl;

import com.rk.inventory_management_system.dtos.*;
import com.rk.inventory_management_system.dtos.OrderITemDto.OrderItemProductDto;
import com.rk.inventory_management_system.dtos.OrderITemDto.OrderItemSupplierDto;
import com.rk.inventory_management_system.dtos.ProductDtos.ProductResponseDto;
import com.rk.inventory_management_system.dtos.ProductDtos.ProductSupplierResponseDto;
import com.rk.inventory_management_system.entities.*;
import com.rk.inventory_management_system.entities.Enums.OrderStatus;
import com.rk.inventory_management_system.entities.Enums.OrderType;
import com.rk.inventory_management_system.exceptions.*;
import com.rk.inventory_management_system.repositories.OrderRepository;
import com.rk.inventory_management_system.schedulers.LowStockScheduler;
import com.rk.inventory_management_system.services.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ModelMapper modelMapper;
    private final CustomerService customerService;
    private final SupplierService supplierService;
    private final ProductService productService;
    private final ProductCategoryService productCategoryService;
    private final LowStockScheduler lowStockScheduler;

    @Override
    @Transactional
    public OrderDto createOrder(OrderDto orderDto) {
        log.info("Starting order creation for order type: {}", orderDto.getOrderType());
        validateOrderDto(orderDto);
        Order order = buildBaseOrder(orderDto);

        try {
            switch (orderDto.getOrderType()) {
                case OrderType.SALE -> processOutwardOrder(order, orderDto);
                case OrderType.PURCHASE -> processInwardOrder(order, orderDto);
                default -> throw new IllegalArgumentException(
                        "Unsupported order type: " + orderDto.getOrderType()
                );
            }

            // Calculate total price before saving
            calculateOrderTotal(order);
            log.info("Order total: {}", order.getTotalPrice());

            order.setCreatedAt(LocalDateTime.now());
            Order savedOrder = orderRepository.save(order);
            log.info("Order created successfully with ID: {}, Total: {}",
                    savedOrder.getId(), savedOrder.getTotalPrice());

            return buildOrderDtoToReturnToController(savedOrder);

        } catch (Exception e) {
            log.error("Failed to create order of type {}: {}", orderDto.getOrderType(), e.getMessage(), e);
            throw new OrderCreationException("Failed to create order: " + e.getMessage());
        }
    }

    // --- Core Processing Methods ---

    private void processOutwardOrder(Order order, OrderDto orderDto) {
        log.info("Processing SALE order with {} items", orderDto.getOrderItems().size());

        Customer customer = getOrCreateCustomer(orderDto);
        order.setCustomer(customer);
        order.setSupplier(null);

        // Pre-validate stock availability for all items
        validateStockAvailabilityForOutward(orderDto.getOrderItems());

        List<OrderItem> orderItems = createOutwardOrderItems(order, orderDto.getOrderItems());
        order.setOrderItems(orderItems);

        log.info("Outward order processed: {} order items created", orderItems.size());
    }

    private void processInwardOrder(Order order, OrderDto orderDto) {
        log.info("Processing inward order with {} items", orderDto.getOrderItems().size());

        Supplier supplier = getOrCreateSupplier(orderDto);
        order.setSupplier(supplier);
        order.setCustomer(null);

        List<OrderItem> orderItems = createInwardOrderItems(order, supplier, orderDto.getOrderItems());
        order.setOrderItems(orderItems);

        log.info("Inward order processed: {} products updated/created", orderItems.size());
    }

    // --- Stock Management Methods ---

    private void validateStockAvailabilityForOutward(List<OrderItemDto> orderItemDtos) {
        List<String> insufficientStockItems = new ArrayList<>();

        for (OrderItemDto itemDto : orderItemDtos) {
            Long productId = itemDto.getProductDto().getProductId();
            int quantityRequested = itemDto.getQuantity();

            ProductResponseDto productDto = productService.getProductById(productId);
            Product product = modelMapper.map(productDto, Product.class);

            if (product == null) {
                throw new ResourceNotFoundException(
                        "Product not found with ID: " + productId
                );
            }

            int totalProductStock = productService.findProductByNameAndBrandName(product.getName(), product.getBrandName())
                    .stream()
                    .mapToInt(Product::getStockQuantity)
                    .sum();

            if (totalProductStock < quantityRequested) {
                insufficientStockItems.add(String.format(
                        "Product: %s - Requested: %d, Available: %d",
                        product.getName(),
                        quantityRequested, totalProductStock
                ));
            }
        }

        if (!insufficientStockItems.isEmpty()) {
            throw new InsufficientStockException(
                    "Insufficient stock for the following items: " + String.join("; ", insufficientStockItems)
            );
        }
    }

    private List<OrderItem> createOutwardOrderItems(Order order, List<OrderItemDto> orderItemDtos) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemDto itemDto : orderItemDtos) {
            OrderItem orderItem = createOutwardOrderItem(order, itemDto);
            orderItems.add(orderItem);

            log.info("Created outward order item for product ID: {} with quantity: {}",
                    itemDto.getProductDto().getProductId(), itemDto.getQuantity());
        }

        return orderItems;
    }

    private OrderItem createOutwardOrderItem(Order order, OrderItemDto itemDto) {
        Long productId = itemDto.getProductDto().getProductId();
        int quantityRequested = itemDto.getQuantity();

        List<ProductDto> productDtos = productService.reduceStockOfProduct(productId, quantityRequested);
        ProductDto productDto = productDtos.getFirst();
        Product product = modelMapper.map(productDto, Product.class);

        // Create order item
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(quantityRequested);

        // Use price from DTO if provided, otherwise use product price
        Double priceToUse = itemDto.getPriceAtOrderTime() != null ?
                itemDto.getPriceAtOrderTime() : product.getSellingPrice();
        orderItem.setPriceAtOrderTime(priceToUse);

        log.debug("Allocated {} units of Product: {})",
                quantityRequested, product.getName());

        return orderItem;
    }

    private List<OrderItem> createInwardOrderItems(Order order, Supplier supplier, List<OrderItemDto> orderItemDtos) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemDto itemDto : orderItemDtos) {
            OrderItem orderItem = createInwardOrderItem(order, supplier, itemDto);
            orderItems.add(orderItem);
        }

        return orderItems;
    }

    private OrderItem createInwardOrderItem(Order order, Supplier newSupplier, OrderItemDto itemDto) {
        Long productId = itemDto.getProductDto().getProductId();
        Double newSellingPrice = itemDto.getProductDto().getSellingPrice();
        log.info("Order selling price: {}",newSellingPrice);
        int quantity = itemDto.getQuantity();


        Product product;

        if (productId != null) {
            ProductResponseDto existingProductDto = productService.getProductById(productId);
            product = mapProductResponseDtoToProduct(existingProductDto, newSupplier, itemDto);

            if (product == null) {
                throw new ResourceNotFoundException("Product not found with ID: " + productId);
            }

            ProductResponseDto updatedProductDto;
            if (existingProductDto.getSupplier().getId().equals(newSupplier.getId())) {
                log.info("Product`s Supplier id: {} equals to Actual Supplier id: {} ", existingProductDto.getSupplier().getId(), newSupplier.getId());
                updatedProductDto = productService.increaseStockOfProduct(product.getId(), quantity);
            } else {
                updatedProductDto = productService.createProduct(modelMapper.map(product, ProductResponseDto.class));
                log.info("Successfully Created new Product: {} with Supplier: {}", updatedProductDto.getName(), updatedProductDto.getSupplier().getId());
            }
            product = modelMapper.map(updatedProductDto, Product.class);

            log.info("Increased stock for product: {} (ID: {}) by {} units",
                    product.getName(), productId, quantity);
        } else {
            throw new IllegalArgumentException(
                    "Product ID is required for inward order items. " +
                            "Create Product then Increase Stock"
            );
        }

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(quantity);

        Double priceToUse = itemDto.getPriceAtOrderTime() != null ?
                itemDto.getPriceAtOrderTime() : product.getSellingPrice();
        orderItem.setPriceAtOrderTime(priceToUse);

        return orderItem;
    }

    // --- Price Calculation ---

    private void calculateOrderTotal(Order order) {
        double totalPrice = order.getOrderItems().stream()
                .mapToDouble(item -> {
                    Double price = item.getPriceAtOrderTime() != null ?
                            item.getPriceAtOrderTime() : item.getProduct().getSellingPrice();
                    return price * item.getQuantity();
                })
                .sum();

        order.setTotalPrice(totalPrice);
        log.debug("Calculated order total: {}", totalPrice);
    }

    // --- Validation Methods ---

    private void validateOrderDto(OrderDto orderDto) {
        log.info("Validating order Dto");
        if (orderDto == null) {
            throw new IllegalArgumentException("Order data cannot be null");
        }

        if (orderDto.getOrderType() == null) {
            throw new IllegalArgumentException("Order type is required");
        }

        if (orderDto.getPaymentType() == null) {
            throw new IllegalArgumentException("Payment type is required");
        }

        if (orderDto.getOrderItems() == null || orderDto.getOrderItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        for (int i = 0; i < orderDto.getOrderItems().size(); i++) {
            OrderItemDto item = orderDto.getOrderItems().get(i);
            validateOrderItem(item, i);
        }

        validateOrderTypeSpecificFields(orderDto);

        log.debug("Order validation completed successfully");
    }

    private void validateOrderItem(OrderItemDto item, int index) {
        log.info("Validating order Items in Order Dto");

        if (item == null) {
            throw new IllegalArgumentException("Order item at index " + index + " cannot be null");
        }

        if (item.getProductDto().getProductId() == null) {
            throw new IllegalArgumentException("Product ID is required for order item at index " + index);
        }

        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive for order item at index " + index);
        }

        if (item.getPriceAtOrderTime() != null && item.getPriceAtOrderTime() < 0) {
            throw new IllegalArgumentException("Price cannot be negative for order item at index " + index);
        }
    }

    private void validateOrderTypeSpecificFields(OrderDto orderDto) {
        log.info("Validating Order type Specific Fields of OrderDto");

        if (orderDto.getOrderType() == OrderType.SALE) {
            if (orderDto.getCustomer().getCustomerId() == null && (
                    orderDto.getCustomer().getEmail() == null
                            || orderDto.getCustomer().getContactNumber() == null
                            || orderDto.getCustomer().getOrders().isEmpty()
            )) {
                throw new IllegalArgumentException("Customer details are required for outward orders");
            }
        } else if (orderDto.getOrderType() == OrderType.PURCHASE) {
            if (orderDto.getSupplier().getId() == null && (
                    orderDto.getSupplier().getName() == null
                            || orderDto.getSupplier().getEmail() == null
                            || orderDto.getSupplier().getContactNumber() == null
                            || orderDto.getSupplier().getProducts().isEmpty()
            )) {
                throw new IllegalArgumentException("Supplier details are required for inward orders");
            }
        }
    }

    // --- Helper Methods ---

    private Order buildBaseOrder(OrderDto orderDto) {
        log.info("Building Order entity");

        return Order.builder()
                .orderType(orderDto.getOrderType())
                .orderStatus(orderDto.getOrderStatus())       //todo can change
                .paymentType(orderDto.getPaymentType())
                .totalPrice(0.0)
                .build();
    }

    private Customer getOrCreateCustomer(OrderDto orderDto) {
        try {
            Customer customer = null;

            if (orderDto.getCustomer().getCustomerId() != null) {
                customer = modelMapper.map(
                        customerService.getCustomerById(orderDto.getCustomer().getCustomerId()),
                        Customer.class
                );
            }

            if (customer == null && orderDto.getCustomer() != null) {
                customer = modelMapper.map(
                        customerService.createCustomer(orderDto.getCustomer()),
                        Customer.class
                );
                log.info("Created new customer: {}", customer.getName());
            }

            if (customer == null) {
                throw new IllegalStateException("Customer details are required for outward orders");
            }

            return customer;
        } catch (Exception e) {
            log.error("Failed to process customer details: {}", e.getMessage());
            throw new CustomerProcessingException("Failed to process customer details", e);
        }
    }

    private Supplier getOrCreateSupplier(OrderDto orderDto) {
        try {
            Supplier supplier = null;

            if (orderDto.getSupplier().getId() != null) {
                supplier = supplierService.getSupplierById(orderDto.getSupplier().getId());
            }

            if (supplier == null && orderDto.getSupplier() != null) {
                supplier = modelMapper.map(
                        supplierService.createSupplier(orderDto.getSupplier()),
                        Supplier.class
                );
                log.info("Created new supplier: {}", supplier.getName());
            }

            if (supplier == null) {
                throw new IllegalStateException("Supplier details are required for inward orders");
            }
            log.info("Supplier of order: : {}", supplier.getName());
            return supplier;
        } catch (Exception e) {
            log.error("Failed to process supplier details: {}", e.getMessage());
            throw new SupplierProcessingException("Failed to process supplier details", e);
        }
    }

    @Override
    @Transactional
    public OrderDto cancelOrder(Long orderId) {
        log.info("Attempting to cancel order with ID: {}", orderId);

        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Order not found with ID: {}", orderId);
                    return new ResourceNotFoundException("Order not found with ID: " + orderId);
                });

        log.info("Found order with ID: {}, current status: {}", orderId, order.getOrderStatus());

        try {
            Order cancelledOrder = updateOrderStatus(order.getId(), OrderStatus.CANCELLED);
            log.info("Order {} successfully cancelled", orderId);
            return buildOrderDtoToReturnToController(cancelledOrder);

        } catch (IllegalStateException e) {
            throw new InvalidOrderCancellationException(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to cancel order {}: {}", orderId, e.getMessage(), e);
            throw new OrderCancellationException("Failed to cancel order with ID: " + orderId, e);
        }
    }

    @Override
    @Transactional
    public OrderDto completeOrder(Long orderId) {
        log.info("Attempting to complete order with ID: {}", orderId);

        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Order not found with ID: {}", orderId);
                    return new ResourceNotFoundException("Order not found with ID: " + orderId);
                });

        log.info("Found order with ID: {}, current status: {}", orderId, order.getOrderStatus());

        try {
            Order completedOrder = updateOrderStatus(order.getId(), OrderStatus.COMPLETED);
            log.info("Order {} successfully completed", orderId);
            return buildOrderDtoToReturnToController(completedOrder);

        } catch (IllegalStateException e) {
            throw new InvalidOrderCompletionException(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to complete order {}: {}", orderId, e.getMessage(), e);
            throw new OrderCompletionException("Failed to complete order with ID: " + orderId, e);
        }
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        if (newStatus == null) {
            throw new IllegalArgumentException("Order status cannot be null");
        }

        OrderStatus currentStatus = order.getOrderStatus();
        log.info("Updating order {} status from {} to {}", order.getId(), currentStatus, newStatus);

        // Validate status transitions
        validateStatusTransition(currentStatus, newStatus);

        // Handle status-specific operations BEFORE updating the status
        switch (newStatus) {
            case COMPLETED -> {
                if (OrderType.SALE.equals(order.getOrderType())) {
                    updateQuantitySoldForCompletedSaleOrder(order);
                }
            }
            case CANCELLED -> {
                // For sale orders, restore stock when cancelling
                if (OrderType.SALE.equals(order.getOrderType())) {
                    restoreStockAndQuantitySoldForCancelledSaleOrder(order, currentStatus);
                } else if (OrderType.PURCHASE.equals(order.getOrderType())) {
                    reverseStockForCancelledPurchaseOrder(order, currentStatus);
                }
            }
        }

        // Update the order status and timestamp
        order.setOrderStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());

        // Set specific timestamps based on status
        if (newStatus == OrderStatus.COMPLETED) {
            order.setCompletedAt(LocalDateTime.now());
        }

        Order updatedOrder = orderRepository.save(order);
        log.info("Order {} status updated successfully to {}", order.getId(), newStatus);

        return updatedOrder;
    }

    /**
     * Reverses stock increase if a PURCHASE order is cancelled after being completed
     */
    private void reverseStockForCancelledPurchaseOrder(Order order, OrderStatus previousStatus) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return;
        }

        log.info("Reversing stock for cancelled purchase order: {} (previous status: {})", order.getId(), previousStatus);

        // ONLY reverse stock if it was previously COMPLETED
        if (previousStatus == OrderStatus.COMPLETED) {
            for (OrderItem orderItem : order.getOrderItems()) {
                Product product = orderItem.getProduct();
                int quantityToReduce = orderItem.getQuantity();

                productService.reduceStockOfProduct(product.getId(), quantityToReduce);

                log.debug("Decreased stock of product {} (ID: {}) by {} units after cancellation of purchase order {}",
                        product.getName(), product.getId(), quantityToReduce, order.getId());
            }
        } else {
            log.debug("No stock adjustment needed as purchase order {} was not previously completed", order.getId());
        }

        log.info("Stock reversal completed for cancelled purchase order: {}", order.getId());
    }

    /**
     * Updates quantitySold for all products in a completed SALE order
     */
    private void updateQuantitySoldForCompletedSaleOrder(Order order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return;
        }

        log.info("Updating quantitySold for completed sale order: {}", order.getId());

        List<Product> productsToUpdate = new ArrayList<>();

        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = orderItem.getProduct();
            int soldQuantity = orderItem.getQuantity();

            // Update quantitySold
            int currentQuantitySold = product.getQuantitySold() != null ? product.getQuantitySold() : 0;
            product.setQuantitySold(currentQuantitySold + soldQuantity);

            productsToUpdate.add(product);

            log.debug("Updated quantitySold for product {} (ID: {}) by {} units. New total: {}",
                    product.getName(), product.getId(), soldQuantity, product.getQuantitySold());
        }

        // Save all updated products
        productService.saveAll(productsToUpdate);

        // Send low stock alerts
        lowStockScheduler.checkAndSendLowStockAlertOrderSpecific(productsToUpdate, order);

        log.info("QuantitySold update completed for order: {}", order.getId());
    }

    /**
     * Restores stock for cancelled SALE orders and adjusts quantitySold if previously completed
     */
    private void restoreStockAndQuantitySoldForCancelledSaleOrder(Order order, OrderStatus previousStatus) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return;
        }

        log.info("Restoring stock for cancelled sale order: {} (previous status: {})", order.getId(), previousStatus);

        List<Product> productsToUpdate = new ArrayList<>();

        for (OrderItem orderItem : order.getOrderItems()) {
            try {
                Product product = orderItem.getProduct();
                int quantityToRestore = orderItem.getQuantity();

                // ALWAYS restore stock for cancelled sale orders (regardless of previous status)
                productService.increaseStockOfProduct(product.getId(), quantityToRestore);
                log.debug("Restored {} units of stock for product ID: {}", quantityToRestore, product.getId());

                // ONLY reverse quantitySold if the order was previously COMPLETED
                if (previousStatus == OrderStatus.COMPLETED) {
                    int currentQuantitySold = product.getQuantitySold() != null ? product.getQuantitySold() : 0;
                    int newQuantitySold = Math.max(0, currentQuantitySold - quantityToRestore);
                    product.setQuantitySold(newQuantitySold);

                    productsToUpdate.add(product);

                    log.debug("Reversed quantitySold for product {} (ID: {}) by {} units. New total: {}",
                            product.getName(), product.getId(), quantityToRestore, newQuantitySold);
                } else {
                    log.debug("No quantitySold adjustment needed for product {} as order was not previously completed",
                            product.getName());
                }

            } catch (Exception e) {
                log.error("Failed to restore stock for product in order item: {}", e.getMessage());
            }
        }

        // Save updated products if any quantitySold changes were made
        if (!productsToUpdate.isEmpty()) {
            productService.saveAll(productsToUpdate);
            log.info("Updated quantitySold for {} products after cancellation", productsToUpdate.size());
        }

        log.info("Stock restoration completed for cancelled order: {}", order.getId());
    }

    // Helper method to validate status transitions
    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        boolean isValidTransition = switch (currentStatus) {
            case CREATED -> newStatus == OrderStatus.PROCESSING ||
                    newStatus == OrderStatus.CANCELLED ||
                    newStatus == OrderStatus.COMPLETED;
            case PROCESSING -> newStatus == OrderStatus.COMPLETED ||
                    newStatus == OrderStatus.CANCELLED;
            case COMPLETED -> false; // No transitions allowed from COMPLETED
            case CANCELLED -> false; // No transitions allowed from CANCELLED
        };

        if (!isValidTransition) {
            throw new IllegalStateException(
                    String.format("Invalid status transition from %s to %s. Order with status %s cannot be changed to %s.",
                            currentStatus, newStatus, currentStatus, newStatus)
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getAllOrdersOfCustomer(Long customerId) {
        log.info("Fetching all orders for customer ID: {}", customerId);

        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }

        CustomerDto customerDto = customerService.getCustomerById(customerId);
        if (customerDto == null) {
            throw new ResourceNotFoundException("Customer not found with ID: " + customerId);
        }

        Customer customer = modelMapper.map(customerDto, Customer.class);
        List<Order> orders = orderRepository.findAllByCustomer(customer);

        log.info("Found {} orders for customer ID: {}", orders.size(), customerId);

        return orders.stream()
                .map(this::buildOrderDtoToReturnToController)
                .toList();
    }

    @Override
    public List<OrderDto> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::buildOrderDtoToReturnToController)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long orderId) {
        log.info("Fetching order with ID: {}", orderId);

        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Order not found with ID: {}", orderId);
                    return new ResourceNotFoundException("Order not found with ID: " + orderId);
                });

        log.info("Successfully retrieved order with ID: {}", orderId);
        return buildOrderDtoToReturnToController(order);
    }

    @Override
    public List<Order> getOrdersByIds(List<Long> orderIds) {
        return orderRepository.findAllById(orderIds);
    }

//
//    public void updateOrderStatus(Long orderId, String orderStatus) {
//        Order order = orderRepository.findById(orderId).orElseThrow(
//                () -> new ResourceNotFoundException("Order not found with id: " + orderId)
//        );
//
//        OrderStatus newStatus = OrderStatus.valueOf(orderStatus);
//        updateOrderStatus(order, newStatus);
//
//        log.info("Order with ID {} updated to status {}", orderId, orderStatus);
//    }

    OrderDto buildOrderDtoToReturnToController(Order order) {
        return OrderDto.builder()
                .orderId(order.getId())
                .createdAt(order.getCreatedAt())
                .orderType(order.getOrderType())
                .orderStatus(order.getOrderStatus())
                .customer(order.getCustomer() != null
                        ? mapCustomerToDto(order.getCustomer())
                        : null)
                .supplier(order.getSupplier() != null
                        ? mapSupplierToDto(order.getSupplier())
                        : null)
                .orderItems(order.getOrderItems() != null
                        ? order.getOrderItems().stream()
                        .filter(Objects::nonNull)
                        .map(this::mapOrderItemToDto)
                        .collect(Collectors.toList())
                        : Collections.emptyList())
                .totalPrice(order.getTotalPrice())
                .paymentType(order.getPaymentType())
                .build();
    }

    private CustomerDto mapCustomerToDto(Customer customer) {
        return CustomerDto.builder()
                .customerId(customer.getId())
                .name(customer.getName())
                .contactNumber(customer.getContactNumber())
                .email(customer.getEmail())
                .address(customer.getAddress())
                .orders(null)  // Explicitly set to null to avoid circular reference
                .createdAt(customer.getCreatedAt())
                .build();
    }

    private SupplierDto mapSupplierToDto(Supplier supplier) {
        return SupplierDto.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .contactNumber(supplier.getContactNumber())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .productsCount(supplier.getProducts().size())
                .products(null)  // Explicitly set to null to avoid lazy loading issues
                .createdAt(supplier.getCreatedAt())
                .build();
    }

    private OrderItemDto mapOrderItemToDto(OrderItem orderItem) {
        return OrderItemDto.builder()
                .orderItemId(orderItem.getId())
                .productDto(orderItem.getProduct() != null
                        ? mapProductToOrderItemProductDto(orderItem.getProduct())
                        : null)
                .orderDto(null)  // Explicitly set to null to avoid circular reference
                .priceAtOrderTime(orderItem.getPriceAtOrderTime())
                .quantity(orderItem.getQuantity())
                .build();
    }

    private OrderItemProductDto mapProductToOrderItemProductDto(Product product) {
        return OrderItemProductDto.builder()
                .productId(product.getId())
                .productCode(product.getProductCode())
                .name(product.getName())
                .brandName(product.getBrandName())
                .description(product.getDescription())
                .sellingPrice(product.getSellingPrice())
                .actualPrice(product.getActualPrice())
                .discount(product.getDiscount())
                .stockQuantity(product.getStockQuantity())
                .quantitySold(product.getQuantitySold())
                .category(product.getCategory() != null
                        ? mapCategoryToDto(product.getCategory())
                        : null)
                .supplier(product.getSupplier() != null
                        ? mapSupplierToDtoForProduct(product.getSupplier())
                        : null)
                .lowStockThreshold(product.getLowStockThreshold())
                .attribute(product.getAttribute())
                .build();
    }

    private ProductCategoryDto mapCategoryToDto(ProductCategory category) {
        return ProductCategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .createdDate(category.getCreatedDate())
                .description(category.getDescription())
                .build();
    }

    private ProductCategory mapProductCategoryDtoToProductCategory(ProductCategoryDto category) {

        return ProductCategory.builder()
                .id(category.getId())
                .name(category.getName())
                .createdDate(category.getCreatedDate())
                .description(category.getDescription())
                .build();
    }

    // Separate supplier mapping for product to avoid deep nesting
    private OrderItemSupplierDto mapSupplierToDtoForProduct(Supplier supplier) {
        return OrderItemSupplierDto.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .contactNumber(supplier.getContactNumber())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .productsCount(null)  // Set to null as shown in your expected output
                .createdAt(supplier.getCreatedAt())
                .build();
    }

    private Product mapProductResponseDtoToProduct(ProductResponseDto dto, Supplier supplier, OrderItemDto orderItemDto) {

        OrderItemProductDto productDto = orderItemDto.getProductDto();
        return Product.builder()
                .name(productDto.getName())
                .id(productDto.getProductId())
                .stockQuantity(orderItemDto.getQuantity())
                .discount(productDto.getDiscount() == null ? 0 : productDto.getDiscount())
                .productCode(productDto.getProductCode())
                .brandName(productDto.getBrandName())
                .attribute(productDto.getAttribute())
                .description(productDto.getDescription())
                .lowStockThreshold(productDto.getLowStockThreshold())
                .sellingPrice(productDto.getSellingPrice())
                .actualPrice(productDto.getActualPrice())
                .discount(productDto.getDiscount())
                .supplier(supplier)
                .category(mapProductCategoryDtoToProductCategory(dto.getCategory()))
                .build();


    }

    private Supplier mapProductSupplierResponseDtoToSupplier(ProductSupplierResponseDto dto) {

        return Supplier.builder()
                .id(dto.getId())
                .name(dto.getName())
                .email(dto.getName())
                .address(dto.getAddress())
                .contactNumber(dto.getContactNumber())
                .build();
    }
}