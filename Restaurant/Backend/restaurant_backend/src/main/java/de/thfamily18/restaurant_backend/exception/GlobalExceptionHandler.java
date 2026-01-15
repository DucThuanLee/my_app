package de.thfamily18.restaurant_backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.core.AuthenticationException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req, Locale locale) {
        log.warn("NOT_FOUND: {}", ex.getMessage());
        return build(ErrorCode.NOT_FOUND, req, locale, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req, Locale locale) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe -> fields.put(fe.getField(), fe.getDefaultMessage()));
        log.warn("VALIDATION_ERROR: {}", fields);
        return build(ErrorCode.VALIDATION_ERROR, req, locale, fields);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuth(AuthenticationException ex, HttpServletRequest req, Locale locale) {
        log.warn("UNAUTHORIZED: {}", ex.getMessage());
        return build(ErrorCode.UNAUTHORIZED, req, locale, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleDenied(AccessDeniedException ex, HttpServletRequest req, Locale locale) {
        log.warn("FORBIDDEN: {}", ex.getMessage());
        return build(ErrorCode.FORBIDDEN, req, locale, null);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException ex, HttpServletRequest req, Locale locale) {
        ErrorCode code = ex.getErrorCode();
        return build(code, req, locale, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req, Locale locale) {
        log.error("INTERNAL_ERROR", ex);
        return build(ErrorCode.INTERNAL_ERROR, req, locale, null);
    }

    private ResponseEntity<ApiError> build(ErrorCode code, HttpServletRequest req, Locale locale, Map<String,String> fieldErrors) {
        String msg = messageSource.getMessage(code.getMessageKey(), null, locale);
        ApiError err = new ApiError(
                code.getStatus().value(),
                code.name(),
                msg,
                req.getRequestURI(),
                LocalDateTime.now(),
                fieldErrors
        );
        return ResponseEntity.status(code.getStatus()).body(err);
    }
}
