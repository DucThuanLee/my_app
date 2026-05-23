package de.thfamily18.restaurant_backend.dto.payment;
import java.math.BigDecimal;
import java.util.UUID;

public record PayPalRefundResponse(
        UUID orderId,
        String paypalRefundId,
        String paypalCaptureId,
        BigDecimal refundedAmount,
        String status
) {}
