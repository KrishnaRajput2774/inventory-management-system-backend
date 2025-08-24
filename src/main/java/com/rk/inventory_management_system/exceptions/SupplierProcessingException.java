package com.rk.inventory_management_system.exceptions;

public class SupplierProcessingException extends RuntimeException{

    public SupplierProcessingException(String message) {
        super(message);
    }

    public SupplierProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
