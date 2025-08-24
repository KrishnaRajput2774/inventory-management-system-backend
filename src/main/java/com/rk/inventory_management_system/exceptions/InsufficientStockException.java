package com.rk.inventory_management_system.exceptions;

public class InsufficientStockException extends RuntimeException{

    public InsufficientStockException(String message) {
        super(message);
    }
}
