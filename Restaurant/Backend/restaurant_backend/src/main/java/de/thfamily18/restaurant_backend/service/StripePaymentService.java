package de.thfamily18.restaurant_backend.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import de.thfamily18.restaurant_backend.dto.payment.CreateStripeIntentResponse;
import de.thfamily18.restaurant_backend.dto.payment.PaymentStatusResponse;
import de.thfamily18.restaurant_backend.entity.Order;
import de.thfamily18.restaurant_backend.entity.PaymentStatus;
import de.thfamily18.restaurant_backend.exception.ResourceNotFoundException;
import de.thfamily18.restaurant_backend.repository.OrderRepository;
import de.thfamily18.restaurant_backend.service.payment.StripeGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StripePaymentService {
    private final StripeGateway stripeGateway;
    private final OrderRepository orderRepo;

    // MVP policy: Payment is only allowed within 30 minutes of order creation.
    private static final Duration ORDER_PAYMENT_TTL = Duration.ofMinutes(30);

    @Transactional
    public CreateStripeIntentResponse createPaymentIntent(UUID orderId) throws StripeException {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // 1) Do not create intents if already paid.
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalArgumentException("Order already paid");
        }

        // 2) Only create intents if the order is PENDING (correct state).
        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Order is not payable in current state: " + order.getPaymentStatus());
        }

        // 3) TTL anti-abuse: orders that are too old will not be allowed to create intents.
        if (order.getCreatedAt() == null ||
                order.getCreatedAt().isBefore(LocalDateTime.now().minus(ORDER_PAYMENT_TTL))) {
            throw new IllegalStateException("Order expired for payment (TTL " + ORDER_PAYMENT_TTL.toMinutes() + " minutes)");
        }

        // 4) Validate totalPrice > 0
        if (order.getTotalPrice() == null || order.getTotalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Invalid order total price");
        }

        // 5) If already has PI -> reuse (idempotent)
        if (order.getStripePaymentIntentId() != null && !order.getStripePaymentIntentId().isBlank()) {
            PaymentIntent existing = stripeGateway.retrievePaymentIntent(order.getStripePaymentIntentId());

            // If Stripe has succeeded but the database hasn't updated (webhook arrived late), still return clientSecret for frontend handling.
            return new CreateStripeIntentResponse(existing.getId(), existing.getClientSecret());
        }

        // Amount in cents
        long amount = order.getTotalPrice()
                .multiply(new BigDecimal("100"))
                .longValueExact();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amount)
                .setCurrency("eur")
                // mapping back in webhook
                .putMetadata("orderId", order.getId().toString())
                .build();

        PaymentIntent pi = stripeGateway.createPaymentIntent(params);

        // Save the PI ID for trace/idempotency.
        order.setStripePaymentIntentId(pi.getId());

        // NOTE: DO NOT set PAID here — a new webhook will do it!
        return new CreateStripeIntentResponse(pi.getId(), pi.getClientSecret());
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getStatus(UUID orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        return new PaymentStatusResponse(
                order.getId(),
                order.getStripePaymentIntentId(),
                order.getPaymentStatus().name(),
                order.getOrderStatus().name()
        );
    }
}

