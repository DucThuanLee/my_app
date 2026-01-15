package de.thfamily18.restaurant_backend.exception;

public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException(String email) {
        super(
                ErrorCode.DUPLICATE_EMAIL,
                "Email already exists: " + email
        );
    }
}