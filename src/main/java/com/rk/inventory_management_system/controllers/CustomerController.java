package com.rk.inventory_management_system.controllers;

import com.rk.inventory_management_system.dtos.CustomerDto;
import com.rk.inventory_management_system.dtos.OrderDto;
import com.rk.inventory_management_system.entities.Customer;
import com.rk.inventory_management_system.services.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping("/create")
    public ResponseEntity<CustomerDto> createCustomer(@RequestBody CustomerDto customerDto) {
        return ResponseEntity.ok(customerService.createCustomer(customerDto));
    }

    @GetMapping("{customerId}")
    public ResponseEntity<CustomerDto> getCustomerById(@PathVariable Long customerId) {
        return ResponseEntity.ok(customerService.getCustomerById(customerId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<CustomerDto>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @GetMapping("/{customerId}/order")
    public ResponseEntity<List<OrderDto>> getCustomerOrders(@PathVariable Long customerId) {
        return ResponseEntity.ok(customerService.getCustomerOrders(customerId));
    }










}
