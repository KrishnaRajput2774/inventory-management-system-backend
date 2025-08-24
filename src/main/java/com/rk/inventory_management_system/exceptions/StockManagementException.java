package com.rk.inventory_management_system.exceptions;

public class StockManagementException extends RuntimeException{

    public StockManagementException(String message) {
        super(message);
    }

    public StockManagementException(String message, Throwable cause) {
        super(message, cause);
    }
}
