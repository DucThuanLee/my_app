package de.thfamily18.restaurant_backend.dto.payment;

import java.util.UUID;

public record PaymentStatusResponse(
        UUID orderId,
        String stripePaymentIntentId,
        String paymentStatus,
        String orderStatus
) {}
