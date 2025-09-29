package com.rk.inventory_management_system.services.impl;

import com.rk.inventory_management_system.dtos.CustomerDto;
import com.rk.inventory_management_system.dtos.OrderDto;
import com.rk.inventory_management_system.entities.Customer;
import com.rk.inventory_management_system.entities.Order;
import com.rk.inventory_management_system.exceptions.ResourceNotFoundException;
import com.rk.inventory_management_system.exceptions.RuntimeConflictException;
import com.rk.inventory_management_system.repositories.CustomerRepository;
import com.rk.inventory_management_system.services.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {


    private final CustomerRepository customerRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public CustomerDto createCustomer(CustomerDto customerDto) {

        log.info("Creating Customer");
        Customer customer = customerRepository.findByEmail(customerDto.getEmail())
                .orElse(null);

        if (customer != null) {
            log.info("Customer Already Present");
            throw new RuntimeConflictException("Customer Already Exists with email: " + customerDto.getEmail());
        }
        Customer savedCustomer = customerRepository.save(modelMapper.map(customerDto, Customer.class));
        log.info("Customer saved Successfully");
        return modelMapper.map(savedCustomer, CustomerDto.class);
    }

    @Override
    public List<CustomerDto> getAllCustomers() {

        List<Customer> customers = customerRepository.findAll();

        return customers.stream().map(
                customer -> modelMapper.map(customer, CustomerDto.class))
                .toList();

    }

    @Override
    public List<OrderDto> getCustomerOrders(Long customerId) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(()->
                        new ResourceNotFoundException("Customer not Found With id: "+customerId));

        List<Order> orders = customer.getOrder();

        return orders.stream().map(
                order -> modelMapper.map(order, OrderDto.class))
                .toList();

    }

    @Override
    public CustomerDto getCustomerById(Long customerId) {

        return modelMapper.map(customerRepository.findById(customerId)
                .orElse(null),CustomerDto.class);

    }
}
