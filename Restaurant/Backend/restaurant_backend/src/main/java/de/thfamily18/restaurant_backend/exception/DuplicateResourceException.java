package de.thfamily18.restaurant_backend.exception;
/**
 * Thrown when a resource already exists (e.g. duplicate product name in same category).
 * This exception is mapped to HTTP 409 CONFLICT by GlobalExceptionHandler.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
