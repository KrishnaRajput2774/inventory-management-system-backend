package com.rk.inventory_management_system.services;

import com.rk.inventory_management_system.dtos.CustomerDto;
import com.rk.inventory_management_system.dtos.OrderDto;

import java.util.List;

public interface CustomerService {

    CustomerDto createCustomer(CustomerDto customerDto);

    List<CustomerDto> getAllCustomers();

    List<OrderDto> getCustomerOrders(Long customerId);

    CustomerDto getCustomerById(Long customerId);


    //TODO update customer Delete customer
}
