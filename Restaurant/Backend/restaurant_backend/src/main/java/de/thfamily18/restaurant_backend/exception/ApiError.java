package de.thfamily18.restaurant_backend.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@AllArgsConstructor
public class ApiError {
    private int status;
    private String errorCode;
    private String message;     // localized DE/EN
    private String path;
    private LocalDateTime timestamp;
    private Map<String, String> fieldErrors; // optional
}
