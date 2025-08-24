package com.rk.inventory_management_system.advice;

import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApiError {

    private HttpStatus httpStatus;
    private String message;
    List<String> subErrors;

}
