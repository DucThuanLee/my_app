package de.thfamily18.restaurant_backend.dto.payment;

import java.util.UUID;

public record RefundResponse(
        UUID orderId,
        String paymentIntentId,
        String refundId,
        String refundStatus,
        String paymentStatus
) {}
