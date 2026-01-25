package de.thfamily18.restaurant_backend.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateRefundRequest(
        @NotNull UUID orderId,
        // null = full refund
        @DecimalMin(value = "0.01", message = "Refund amount must be > 0")
        BigDecimal amount,
        // optional: customer_request, duplicate, fraud, etc.
        String reason
) {}
