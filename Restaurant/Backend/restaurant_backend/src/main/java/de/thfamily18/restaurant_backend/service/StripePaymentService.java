package de.thfamily18.restaurant_backend.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import de.thfamily18.restaurant_backend.dto.payment.CreateStripeIntentResponse;
import de.thfamily18.restaurant_backend.dto.payment.PaymentStatusResponse;
import de.thfamily18.restaurant_backend.entity.Order;
import de.thfamily18.restaurant_backend.entity.PaymentStatus;
import de.thfamily18.restaurant_backend.exception.ResourceNotFoundException;
import de.thfamily18.restaurant_backend.repository.OrderRepository;
import de.thfamily18.restaurant_backend.service.payment.StripeGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentService {
    private final StripeGateway stripeGateway;
    private final OrderRepository orderRepo;
    @Value("${stripe.secretKey:}")
    private String stripeSecretKey;
    // MVP policy: Payment is only allowed within 30 minutes of order creation.
    @Value("${app.payment.ttlMinutes:30}")
    private long ttlMinutes;

    @Transactional
    public CreateStripeIntentResponse createPaymentIntent(UUID orderId) throws StripeException {
        log.info("ttlMinutes {}", ttlMinutes);
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
        Duration ttl = Duration.ofMinutes(ttlMinutes);
        if (order.getCreatedAt() == null ||
                order.getCreatedAt().isBefore(LocalDateTime.now().minus(ttl))) {
            throw new IllegalStateException("Order expired for payment (TTL " + ttl.toMinutes() + " minutes)");
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

        // 6. Amount in cents
        // robust rounding (e.g. 7.8 -> 780, 7.805 -> 781)
        long amount = toCents(order.getTotalPrice());

        // Build PaymentIntent params
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amount)
                .setCurrency("eur")
                // only for testing
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .setAllowRedirects(
                                        PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER
                                )
                                .build()
                )
                // enables card + local payment methods automatically (recommended)
//                .setAutomaticPaymentMethods(
//                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
//                                .setEnabled(true)
//                                .build()
//                )
                // mapping back in webhook
                .putMetadata("orderId", order.getId().toString())
                .setDescription("Order " + order.getId())
                .build();
        // 7) Stripe idempotency key: same order -> same PI (Stripe guarantees)
        String idempotencyKey = "order:" + order.getId();
        PaymentIntent pi = stripeGateway.createPaymentIntent(params, requestOptions(idempotencyKey));

        // Save the PI ID for trace/idempotency.
        order.setStripePaymentIntentId(pi.getId());
        orderRepo.save(order);
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

    private long toCents(BigDecimal eur) {
        // robust rounding (e.g. 7.8 -> 780, 7.805 -> 781)
        return eur.movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private RequestOptions requestOptions(String idempotencyKey) {
        RequestOptions.RequestOptionsBuilder b = RequestOptions.builder();

        // If you used Stripe.apiKey globally, you can omit setApiKey here.
        // But setting it here is safer for tests/profiles.
        if (stripeSecretKey != null && !stripeSecretKey.isBlank()) {
            b.setApiKey(stripeSecretKey);
        }
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            b.setIdempotencyKey(idempotencyKey);
        }
        return b.build();
    }
}

