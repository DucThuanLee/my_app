package de.thfamily18.restaurant_backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Locale;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request,
            Locale locale) {

        log.warn("NOT_FOUND: {}", ex.getMessage());

        return buildError(ErrorCode.NOT_FOUND, request, locale);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request,
            Locale locale) {

        log.warn("VALIDATION_ERROR: {}", ex.getMessage());

        return buildError(ErrorCode.VALIDATION_ERROR, request, locale);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(
            Exception ex,
            HttpServletRequest request,
            Locale locale) {

        log.error("INTERNAL_ERROR", ex);

        return buildError(ErrorCode.INTERNAL_ERROR, request, locale);
    }

    private ResponseEntity<ApiError> buildError(
            ErrorCode errorCode,
            HttpServletRequest request,
            Locale locale) {

        String message = messageSource.getMessage(
                errorCode.getMessageKey(),
                null,
                locale
        );

        ApiError error = new ApiError(
                errorCode.getStatus().value(),
                errorCode.name(),
                message,
                request.getRequestURI(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(errorCode.getStatus()).body(error);
    }
}
