package de.thfamily18.restaurant_backend.exception;

import lombok.Getter;

@Getter
public abstract class BusinessException extends RuntimeException {
x
    private final ErrorCode errorCode;

    protected BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

}
