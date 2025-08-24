package com.rk.inventory_management_system.exceptions;


public class InvalidOrderCancellationException extends RuntimeException {
    public InvalidOrderCancellationException(String message) {
        super(message);
    }
}
