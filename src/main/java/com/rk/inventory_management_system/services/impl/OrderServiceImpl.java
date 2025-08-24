package com.rk.inventory_management_system.services.impl;

import com.rk.inventory_management_system.dtos.*;
import com.rk.inventory_management_system.entities.*;
import com.rk.inventory_management_system.entities.Enums.OrderStatus;
import com.rk.inventory_management_system.entities.Enums.OrderType;
import com.rk.inventory_management_system.exceptions.*;
import com.rk.inventory_management_system.repositories.OrderRepository;
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
        log.info("Processing outward order with {} items", orderDto.getOrderItems().size());

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

            // Get product by ID
            ProductDto productDto = productService.getProductById(productId);
            Product product = modelMapper.map(productDto,Product.class);

            //manually setting up cause of ModelMapper

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

        //TODO here if the product with specific id is not present but same product could be present with different supplier
        //TODO let frontend send ProductRequestDto which would contain name and brand according to which we can find product in DB

        ProductDto productDto = productDtos.getFirst();
        Product product = modelMapper.map(productDto, Product.class); // we want any product which has reduced stock

        // Reduce stock

        // Create order item
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product); //here we specifically don`t know which supplier product was sell we just product name and brand
        orderItem.setQuantity(quantityRequested);

        // Use price from DTO if provided, otherwise use product price
        Double priceToUse = itemDto.getPriceAtOrderTime() != null ?
                itemDto.getPriceAtOrderTime() : product.getPrice();
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

    private OrderItem createInwardOrderItem(Order order, Supplier supplier, OrderItemDto itemDto) {
        Long productId = itemDto.getProductDto().getProductId();
        int quantity = itemDto.getQuantity();

        Product product;

        if (productId != null) {
            // Update existing product
            ProductDto productDto  = productService.getProductById(productId);
            product = modelMapper.map(productDto,Product.class);

            if (product == null) {
                throw new ResourceNotFoundException("Product not found with ID: " + productId);
            }

            // Verify the product belongs to the correct supplier
            if (!product.getSupplier().getId().equals(supplier.getId())) {
                throw new IllegalArgumentException(
                        "Product ID " + productId + " does not belong to supplier: " + supplier.getName()
                );
            }
            //TODO here can Be error
            ProductDto updatedProductDto = productService.increaseStockOfProduct(product.getId(), quantity);
            product = modelMapper.map(updatedProductDto, Product.class);

            log.info("Increased stock for product: {} (ID: {}) by {} units",
                    product.getName(), productId, quantity);
        } else {
            // For inward orders, if productId is null, we need additional product details
            // This would require extending your DTO or handling it differently
            throw new IllegalArgumentException(
                    "Product ID is required for inward order items. " +
                            "Create Product then Increase Stock"
            );
        }

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(quantity);

        // Use price from DTO if provided, otherwise use product price
        Double priceToUse = itemDto.getPriceAtOrderTime() != null ?
                itemDto.getPriceAtOrderTime() : product.getPrice();
        orderItem.setPriceAtOrderTime(priceToUse);

        return orderItem;
    }

// --- Price Calculation ---

    private void calculateOrderTotal(Order order) {
        double totalPrice = order.getOrderItems().stream()
                .mapToDouble(item -> {
                    Double price = item.getPriceAtOrderTime() != null ?
                            item.getPriceAtOrderTime() : item.getProduct().getPrice();
                    return price * item.getQuantity();
                })
                .sum();

        order.setTotalPrice(totalPrice);
        log.debug("Calculated order total: {}", totalPrice);
    }

// --- Validation Methods ---

    private void validateOrderDto(OrderDto orderDto) {
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

        // Validate each order item
        for (int i = 0; i < orderDto.getOrderItems().size(); i++) {
            OrderItemDto item = orderDto.getOrderItems().get(i);
            validateOrderItem(item, i);
        }

        // Type-specific validations
        validateOrderTypeSpecificFields(orderDto);

        log.debug("Order validation completed successfully");
    }

    private void validateOrderItem(OrderItemDto item, int index) {
        if (item == null) {
            throw new IllegalArgumentException("Order item at index " + index + " cannot be null");
        }

        if (item.getProductDto().getProductId() == null) {
            throw new IllegalArgumentException("Product ID is required for order item at index " + index);
        }

        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive for order item at index " + index);
        }

        // Validate price if provided
        if (item.getPriceAtOrderTime() != null && item.getPriceAtOrderTime() < 0) {
            throw new IllegalArgumentException("Price cannot be negative for order item at index " + index);
        }
    }

    private void validateOrderTypeSpecificFields(OrderDto orderDto) {
        if (orderDto.getOrderType() == OrderType.SALE) {
            if (orderDto.getCustomerDto().getCustomerId() == null && (
                    orderDto.getCustomerDto().getEmail() == null
                    || orderDto.getCustomerDto().getContactNumber() == null
                    || orderDto.getCustomerDto().getOrders().isEmpty()
            )) {
                throw new IllegalArgumentException("Customer details are required for outward orders");
            }
        } else if (orderDto.getOrderType() == OrderType.PURCHASE) {
            if (orderDto.getSupplierDto().getId() == null && (
                    orderDto.getSupplierDto().getName() == null
                    || orderDto.getSupplierDto().getEmail() == null
                    || orderDto.getSupplierDto().getContactNumber()==null
                    || orderDto.getSupplierDto().getProducts().isEmpty()
            )) {
                throw new IllegalArgumentException("Supplier details are required for inward orders");
            }
        }
    }

// --- Helper Methods ---

    private Order buildBaseOrder(OrderDto orderDto) {
        return Order.builder()
                .orderType(orderDto.getOrderType())
                .orderStatus(OrderStatus.CREATED)
                .paymentType(orderDto.getPaymentType())
                .totalPrice(0.0)
                .build();
    }

    private Customer getOrCreateCustomer(OrderDto orderDto) {
        try {
            Customer customer = null;

            if (orderDto.getCustomerDto().getCustomerId() != null) {
                customer = modelMapper.map(
                        customerService.getCustomerById(orderDto.getCustomerDto().getCustomerId()),
                        Customer.class
                );
            }

            if (customer == null && orderDto.getCustomerDto() != null) {
                customer = modelMapper.map(
                        customerService.createCustomer(orderDto.getCustomerDto()),
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

            if (orderDto.getSupplierDto().getId() != null) {
                supplier = supplierService.getSupplierById(orderDto.getSupplierDto().getId());
            }

            if (supplier == null && orderDto.getSupplierDto() != null) {
                supplier = modelMapper.map(
                        supplierService.createSupplier(orderDto.getSupplierDto()),
                        Supplier.class
                );
                log.info("Created new supplier: {}", supplier.getName());
            }

            if (supplier == null) {
                throw new IllegalStateException("Supplier details are required for inward orders");
            }

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
            // Use the generalized updateOrderStatus method
            Order cancelledOrder = updateOrderStatus(order, OrderStatus.CANCELLED);

            log.info("Order {} successfully cancelled", orderId);

            return buildOrderDtoToReturnToController(cancelledOrder);

        } catch (IllegalStateException e) {
            // Convert status transition errors to more specific exceptions
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
            // Use the generalized updateOrderStatus method
            Order completedOrder = updateOrderStatus(order, OrderStatus.COMPLETED);

            log.info("Order {} successfully completed", orderId);

            return buildOrderDtoToReturnToController(completedOrder);


        } catch (IllegalStateException e) {
            // Convert status transition errors to more specific exceptions
            throw new InvalidOrderCompletionException(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to complete order {}: {}", orderId, e.getMessage(), e);
            throw new OrderCompletionException("Failed to complete order with ID: " + orderId, e);
        }
    }

    @Transactional
    public Order updateOrderStatus(Order order, OrderStatus newStatus) {
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

        order.setOrderStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());

        // Set specific timestamps based on status
        switch (newStatus) {
            case COMPLETED -> order.setCompletedAt(LocalDateTime.now());
            case CANCELLED -> {
                // For outward orders, restore stock when cancelling
                if (OrderType.SALE.equals(order.getOrderType())) {
                    restoreStockForCancelledOrder(order);
                }
            }
        }

        Order updatedOrder = orderRepository.save(order);
        log.info("Order {} status updated successfully to {}", order.getId(), newStatus);

        return updatedOrder;
    }

    // Helper method to validate status transitions
    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        // Define valid transitions
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

    // Helper method to restore stock for cancelled outward orders
    private void restoreStockForCancelledOrder(Order order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return;
        }

        log.info("Restoring stock for cancelled outward order: {}", order.getId());

        for (OrderItem orderItem : order.getOrderItems()) {
            try {
                Long productId = orderItem.getProduct().getId();
                int quantityToRestore = orderItem.getQuantity();

                productService.increaseStockOfProduct(productId, quantityToRestore);

                log.debug("Restored {} units for product ID: {}", quantityToRestore, productId);
            } catch (Exception e) {
                log.error("Failed to restore stock for product in order item: {}", e.getMessage());
                // Continue with other items but log the error
            }
        }

        log.info("Stock restoration completed for order: {}", order.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getAllOrdersOfCustomer(Long customerId) {
        log.info("Fetching all orders for customer ID: {}", customerId);

        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }

        // Verify customer exists
        CustomerDto customerDto = customerService.getCustomerById(customerId);
        if (customerDto == null) {
            throw new ResourceNotFoundException("Customer not found with ID: " + customerId);
        }

        Customer customer = modelMapper.map(customerDto, Customer.class);
        List<Order> orders = orderRepository.findAllByCustomer(customer);

        log.info("Found {} orders for customer ID: {}", orders.size(), customerId);

        return orders.stream()
                .map(order ->{
                    return buildOrderDtoToReturnToController(order);
                })
                .toList();
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

    OrderDto buildOrderDtoToReturnToController(Order order) {
        return OrderDto.builder()
                .orderId(order.getId())
                .orderType(order.getOrderType())
                .orderStatus(order.getOrderStatus())
                .customerDto(order.getCustomer() != null
                        ? modelMapper.map(order.getCustomer(), CustomerDto.class)
                        : null)
                .supplierDto(order.getSupplier() != null
                        ? modelMapper.map(order.getSupplier(), SupplierDto.class)
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
    private OrderItemDto mapOrderItemToDto(OrderItem orderItem) {
        return OrderItemDto.builder()
                .orderItemId(orderItem.getId())
                .productDto(orderItem.getProduct() != null
                        ? modelMapper.map(orderItem.getProduct(), ProductDto.class)
                        : null)
                .priceAtOrderTime(orderItem.getPriceAtOrderTime())
                .quantity(orderItem.getQuantity())
                .build();
    }
}
