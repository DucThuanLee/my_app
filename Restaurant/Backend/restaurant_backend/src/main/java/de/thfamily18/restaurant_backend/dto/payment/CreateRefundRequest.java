package de.thfamily18.restaurant_backend.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateRefundRequest(
        @NotNull
        UUID orderId,
        @NotNull
        UUID requestId,
        // null = refund all remaining amount
        @DecimalMin(
                value = "0.01",
                message = "Refund amount must be greater than or equal to 0.01"
        )
        BigDecimal amount,
        String reason
) {}