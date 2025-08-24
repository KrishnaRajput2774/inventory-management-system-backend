package com.rk.inventory_management_system.exceptions;

public class InvalidOrderCompletionException extends RuntimeException{
    public InvalidOrderCompletionException(String message) {
        super(message);
    }
}
