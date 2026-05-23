package de.thfamily18.restaurant_backend.dto.payment;

import de.thfamily18.restaurant_backend.entity.PaymentStatus;
import de.thfamily18.restaurant_backend.entity.RefundProviderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RefundResponse(
        UUID orderId,
        String paymentIntentId,
        String refundId,
        RefundProviderStatus refundStatus,
        PaymentStatus paymentStatus,
        BigDecimal refundedAmount,
        LocalDateTime requestedAt
) {}
