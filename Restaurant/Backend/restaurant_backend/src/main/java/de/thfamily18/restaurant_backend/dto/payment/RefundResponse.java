package de.thfamily18.restaurant_backend.dto.payment;

import de.thfamily18.restaurant_backend.entity.PaymentStatus;
import de.thfamily18.restaurant_backend.entity.RefundStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record RefundResponse(
        String refundId,
        RefundStatus refundStatus,
        UUID orderId,
        String paymentIntentId,
        PaymentStatus paymentStatus,
        LocalDateTime requestedAt
) {}
