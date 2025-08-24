package com.rk.inventory_management_system.advice;


import com.rk.inventory_management_system.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFoundException(ResourceNotFoundException exception) {

        ApiError apiError = ApiError.builder()
                .message(exception.getMessage())
                .httpStatus(HttpStatus.NOT_FOUND)
                .build();

        return new ResponseEntity<>(apiError,HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RuntimeConflictException.class)
    public ResponseEntity<ApiError> handleRuntimeConflictException(RuntimeConflictException exception) {

        ApiError apiError = ApiError.builder()
                .message(exception.getMessage())
                .httpStatus(HttpStatus.CONFLICT)
                .build();

        return new ResponseEntity<>(apiError,HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiError> handleOutOfStockException(InsufficientStockException exception) {

        ApiError apiError = ApiError.builder()
                .message(exception.getMessage())
                .httpStatus(HttpStatus.CONFLICT)
                .build();

        return new ResponseEntity<>(apiError,HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException exception) {

        ApiError apiError = ApiError.builder()
                .message(exception.getMessage())
                .httpStatus(HttpStatus.BAD_REQUEST)
                .build();

        return new ResponseEntity<>(apiError,HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidOrderCancellationException.class)
    public ResponseEntity<ApiError> handleInvalidOrderCancellationException(InvalidOrderCancellationException exception) {

        ApiError apiError = ApiError.builder()
                .message(exception.getMessage())
                .httpStatus(HttpStatus.CONFLICT)
                .build();

        return new ResponseEntity<>(apiError,HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidOrderCompletionException.class)
    public ResponseEntity<ApiError> handleInvalidOrderCompletionException(InvalidOrderCompletionException exception) {

        ApiError apiError = ApiError.builder()
                .message(exception.getMessage())
                .httpStatus(HttpStatus.CONFLICT)
                .build();

        return new ResponseEntity<>(apiError,HttpStatus.CONFLICT);
    }

    @ExceptionHandler(OrderCreationException.class)
    public ResponseEntity<ApiError> handleOrderCreationException(OrderCreationException exception) {

        ApiError apiError = ApiError.builder()
                .message(exception.getMessage())
                .httpStatus(HttpStatus.BAD_REQUEST)
                .build();

        return new ResponseEntity<>(apiError,HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CustomerProcessingException.class)
    public ResponseEntity<ApiError> handleCustomerProcessingException(CustomerProcessingException exception) {

        ApiError apiError = ApiError.builder()
                .message(exception.getMessage())
                .httpStatus(HttpStatus.BAD_REQUEST)
                .build();

        return new ResponseEntity<>(apiError,HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SupplierProcessingException.class)
    public ResponseEntity<ApiError> handleSupplierProcessingException(SupplierProcessingException exception) {

        ApiError apiError = ApiError.builder()
                .message(exception.getMessage())
                .httpStatus(HttpStatus.BAD_REQUEST)
                .build();

        return new ResponseEntity<>(apiError,HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OrderCompletionException.class)
    public ResponseEntity<ApiError> handleOrderCompletionException(OrderCompletionException exception) {

        ApiError apiError = ApiError.builder()
                .message(exception.getMessage())
                .httpStatus(HttpStatus.BAD_REQUEST)
                .build();

        return new ResponseEntity<>(apiError,HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OrderCancellationException.class)
    public ResponseEntity<ApiError> handleOrderCancellationException(OrderCancellationException exception) {

        ApiError apiError = ApiError.builder()
                .message(exception.getMessage())
                .httpStatus(HttpStatus.BAD_REQUEST)
                .build();

        return new ResponseEntity<>(apiError,HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OrderItemManagementException.class)
    public ResponseEntity<ApiError> handleOrderItemManagementException(OrderItemManagementException exception) {

        ApiError apiError = ApiError.builder()
                .message(exception.getMessage())
                .httpStatus(HttpStatus.BAD_REQUEST)
                .build();

        return new ResponseEntity<>(apiError,HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(StockManagementException.class)
    public ResponseEntity<ApiError> handleStockManagementException(StockManagementException exception) {

        ApiError apiError = ApiError.builder()
                .message(exception.getMessage())
                .httpStatus(HttpStatus.BAD_REQUEST)
                .build();

        return new ResponseEntity<>(apiError,HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalStateException(IllegalStateException exception) {

        ApiError apiError = ApiError.builder()
                .message(exception.getMessage())
                .httpStatus(HttpStatus.BAD_REQUEST)
                .build();

        return new ResponseEntity<>(apiError,HttpStatus.BAD_REQUEST);
    }

}
