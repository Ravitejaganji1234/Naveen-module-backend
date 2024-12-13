package com.middleware.leave_approval_system.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


// This class handles global exceptions for the application
@RestControllerAdvice // This annotation indicates that this class will handle exceptions globally for all controllers
public class GlobalExceptionHandler {

    // This method is triggered when a ResourceNotFoundException is thrown in any controller
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> resourceNotFoundException(ResourceNotFoundException e, WebRequest request) {
        Map<String, Object> responseBody=new HashMap<>();
        responseBody.put("timestamp", LocalDateTime.now());
        responseBody.put("message", e.getMessage());
        responseBody.put("details", request.getDescription(false));
        return new ResponseEntity<>(responseBody, HttpStatus.NOT_FOUND);
    }
}
