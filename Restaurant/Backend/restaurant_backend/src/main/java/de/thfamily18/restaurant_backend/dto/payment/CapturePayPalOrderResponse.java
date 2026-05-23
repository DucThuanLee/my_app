package de.thfamily18.restaurant_backend.dto.payment;
import de.thfamily18.restaurant_backend.entity.PaymentStatus;
import java.util.UUID;

public record CapturePayPalOrderResponse(
        UUID orderId,
        String paypalOrderId,
        String paypalCaptureId,
        PaymentStatus paymentStatus
) {}
