package com.rk.inventory_management_system.exceptions;

public class CustomerProcessingException extends RuntimeException{

    public CustomerProcessingException(String message) {
        super(message);
    }

    public CustomerProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
