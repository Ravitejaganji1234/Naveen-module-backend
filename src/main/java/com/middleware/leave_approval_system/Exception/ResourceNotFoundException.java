package com.middleware.leave_approval_system.Exception;

import lombok.Data;
import lombok.NoArgsConstructor;

// Custom exception class extending RuntimeException to represent resource not found errors
@NoArgsConstructor
@Data
public class ResourceNotFoundException extends RuntimeException{

    // Message to store the custom error message
    private String message;

    // Constructor accepting a single message parameter
    public ResourceNotFoundException(String message) {
        this.message = message;
    }

    // Constructor accepting two message parameters:
    // one for the super class and another to set the custom message
    public ResourceNotFoundException(String message, String message1) {
        super(message);
        this.message = message1;
    }

    public ResourceNotFoundException(String message, Throwable cause, String message1) {
        super(message, cause);
        this.message = message1;
    }

    public ResourceNotFoundException(Throwable cause, String message) {
        super(cause);
        this.message = message;
    }

    public ResourceNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, String message1) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.message = message1;
    }
}
