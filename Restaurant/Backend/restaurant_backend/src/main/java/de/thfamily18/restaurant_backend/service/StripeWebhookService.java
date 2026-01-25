package de.thfamily18.restaurant_backend.service;

import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import de.thfamily18.restaurant_backend.entity.Order;
import de.thfamily18.restaurant_backend.entity.PaymentStatus;
import de.thfamily18.restaurant_backend.exception.ResourceNotFoundException;
import de.thfamily18.restaurant_backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookService {
    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            "payment_intent.succeeded",
            "payment_intent.payment_failed",
            "payment_intent.canceled",
            "charge.refunded" // refund confirmed
            // (optional later) "refund.updated"

    );
    private final OrderRepository orderRepo;

    @Value("${stripe.webhookSecret}")
    private String webhookSecret;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handle(String payload, String sigHeader) {
        // Fail-fast in production if missing secret (must verify)
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException("Missing stripe.webhookSecret (STRIPE_WEBHOOK_SECRET)");
        }
        final Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Stripe signature", e);
        }

        String type = event.getType();
        if (!SUPPORTED_EVENTS.contains(type)) {
            // ignore other events
            return;
        }

        // Parse raw payload (reliable)
        final JsonNode obj;
        try {
            obj = objectMapper.readTree(payload).path("data").path("object");
        } catch (Exception e) {
            log.warn("Cannot parse webhook payload. eventId={}, type={}", event.getId(), type, e);
            return; // return 200 OK to prevent endless retries; your controller returns ok anyway
        }

        log.info("Stripe webhook received. type={}, eventId={}", type, event.getId());

        // Route by event type
        if (type.startsWith("payment_intent.")) {
            handlePaymentIntentEvent(type, event.getId(), obj);
            return;
        }

        if ("charge.refunded".equals(type)) {
            handleChargeRefunded(event.getId(), obj);
        }
    }

    private void handlePaymentIntentEvent(String type, String eventId, JsonNode obj) {
        final String piId = obj.path("id").asText(null);
        final String orderIdStr = obj.path("metadata").path("orderId").asText(null);

        log.info("PI event. type={}, eventId={}, piId={}, orderId={}", type, eventId, piId, orderIdStr);

        if (orderIdStr == null || orderIdStr.isBlank()) {
            log.warn("Missing metadata.orderId. eventId={}, piId={}", eventId, piId);
            return;
        }

        final UUID orderId;
        try {
            orderId = UUID.fromString(orderIdStr);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid orderId UUID in metadata. orderId={}, eventId={}, piId={}", orderIdStr, eventId, piId);
            return;
        }

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        // Idempotency / state rules
        if (order.getPaymentStatus() == PaymentStatus.REFUNDED) {
            // if already refunded, ignore PI updates
            log.info("Order already REFUNDED, ignoring PI event. orderId={}, type={}, piId={}", orderId, type, piId);
            return;
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID &&
                ("payment_intent.payment_failed".equals(type) || "payment_intent.canceled".equals(type))) {
            // ignore late/out-of-order fail/cancel after success
            log.info("Order already PAID, ignoring late PI fail/cancel. orderId={}, type={}, piId={}", orderId, type, piId);
            return;
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID && "payment_intent.succeeded".equals(type)) {
            log.info("Order already PAID, ignoring duplicate succeeded. orderId={}, piId={}", orderId, piId);
            return;
        }

        // Optional safety: mismatch PI id
        if (order.getStripePaymentIntentId() != null && !order.getStripePaymentIntentId().isBlank()
                && piId != null && !piId.isBlank()
                && !order.getStripePaymentIntentId().equals(piId)) {
            log.warn("PaymentIntent mismatch. orderId={}, dbPiId={}, webhookPiId={}",
                    orderId, order.getStripePaymentIntentId(), piId);
            // choose strict/relaxed. We'll proceed (relaxed) and overwrite.
        }

        // Update state
        if ("payment_intent.succeeded".equals(type)) {
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
            order.setStripePaymentIntentId(piId);
        } else if ("payment_intent.payment_failed".equals(type)) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setStripePaymentIntentId(piId);
        } else if ("payment_intent.canceled".equals(type)) {
            order.setPaymentStatus(PaymentStatus.CANCELED);
            order.setStripePaymentIntentId(piId);
        }

        orderRepo.save(order);

        log.info("Order updated from PI event. orderId={}, paymentStatus={}, piId={}",
                orderId, order.getPaymentStatus(), piId);
    }

    private void handleChargeRefunded(String eventId, JsonNode obj) {
        // charge.refunded object contains payment_intent
        final String paymentIntentId = obj.path("payment_intent").asText(null);
        final String chargeId = obj.path("id").asText(null);

        log.info("Charge refunded. eventId={}, chargeId={}, paymentIntentId={}", eventId, chargeId, paymentIntentId);

        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            log.warn("Missing payment_intent in charge.refunded. eventId={}, chargeId={}", eventId, chargeId);
            return;
        }

        Order order = orderRepo.findByStripePaymentIntentId(paymentIntentId)
                .orElse(null);

        if (order == null) {
            log.warn("Order not found by stripePaymentIntentId. paymentIntentId={}, eventId={}", paymentIntentId, eventId);
            return;
        }

        if (order.getPaymentStatus() == PaymentStatus.REFUNDED) {
            log.info("Order already REFUNDED, ignoring duplicate charge.refunded. orderId={}, piId={}",
                    order.getId(), paymentIntentId);
            return;
        }

        // Mark refunded (source of truth: webhook)
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        // If you added fields:
        order.setRefundedAt(LocalDateTime.now());

        orderRepo.save(order);

        log.info("Order marked REFUNDED. orderId={}, piId={}", order.getId(), paymentIntentId);
    }

}

