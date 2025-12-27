package de.thfamily18.restaurant_backend.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ApiError {

    private int status;
    private String errorCode;   // NOT_FOUND, VALIDATION_ERROR
    private String message;     // Translated in DE / EN
    private String path;
    private LocalDateTime timestamp;
}
