package de.thfamily18.restaurant_backend.service;

import com.stripe.model.Event;
import com.stripe.net.Webhook;
import de.thfamily18.restaurant_backend.entity.Order;
import de.thfamily18.restaurant_backend.entity.PaymentStatus;
import de.thfamily18.restaurant_backend.exception.ResourceNotFoundException;
import de.thfamily18.restaurant_backend.notification.NotificationService;
import de.thfamily18.restaurant_backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
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
    );

    private final OrderRepository orderRepo;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Value("${stripe.webhookSecret}")
    private String webhookSecret;

    @Transactional
    public void handle(String payload, String sigHeader) {
        // Fail-fast if missing secret (signature verification must be enabled in production).
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
            // Ignore unrelated events.
            return;
        }

        final JsonNode obj;
        try {
            obj = objectMapper.readTree(payload).path("data").path("object");
        } catch (Exception e) {
            log.warn("Cannot parse webhook payload. eventId={}, type={}", event.getId(), type, e);
            // Return 200 OK to prevent Stripe retry storms; controller should always respond OK.
            return;
        }

        log.info("Stripe webhook received. type={}, eventId={}", type, event.getId());

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
            log.info("Order already REFUNDED, ignoring PI event. orderId={}, type={}, piId={}", orderId, type, piId);
            return;
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID &&
                ("payment_intent.payment_failed".equals(type) || "payment_intent.canceled".equals(type))) {
            log.info("Order already PAID, ignoring late PI fail/cancel. orderId={}, type={}, piId={}", orderId, type, piId);
            return;
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID && "payment_intent.succeeded".equals(type)) {
            log.info("Order already PAID, ignoring duplicate succeeded. orderId={}, piId={}", orderId, piId);
            return;
        }

        // Optional safety: PaymentIntent mismatch (relaxed: overwrite).
        if (order.getStripePaymentIntentId() != null && !order.getStripePaymentIntentId().isBlank()
                && piId != null && !piId.isBlank()
                && !order.getStripePaymentIntentId().equals(piId)) {
            log.warn("PaymentIntent mismatch. orderId={}, dbPiId={}, webhookPiId={}",
                    orderId, order.getStripePaymentIntentId(), piId);
        }

        // Update state
        if ("payment_intent.succeeded".equals(type)) {
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
            order.setStripePaymentIntentId(piId);

            // Enqueue notification after state is updated (still inside DB transaction).
            enqueuePaymentSucceeded(order);

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
        final String paymentIntentId = obj.path("payment_intent").asText(null);
        final String chargeId = obj.path("id").asText(null);

        log.info("Charge refunded. eventId={}, chargeId={}, paymentIntentId={}", eventId, chargeId, paymentIntentId);

        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            log.warn("Missing payment_intent in charge.refunded. eventId={}, chargeId={}", eventId, chargeId);
            return;
        }

        Order order = orderRepo.findByStripePaymentIntentId(paymentIntentId).orElse(null);

        if (order == null) {
            log.warn("Order not found by stripePaymentIntentId. paymentIntentId={}, eventId={}", paymentIntentId, eventId);
            return;
        }

        if (order.getPaymentStatus() == PaymentStatus.REFUNDED) {
            log.info("Order already REFUNDED, ignoring duplicate charge.refunded. orderId={}, piId={}",
                    order.getId(), paymentIntentId);
            return;
        }

        // Mark refunded (webhook is the source of truth).
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        order.setRefundedAt(LocalDateTime.now());
        orderRepo.save(order);

        // Enqueue refund notification
        enqueueRefundSucceeded(order, chargeId);

        log.info("Order marked REFUNDED. orderId={}, piId={}", order.getId(), paymentIntentId);
    }

    private void enqueuePaymentSucceeded(Order order) {
        // If user is guest, decide where email comes from:
        // - If you store customer email in Order => use it
        // - Else if user exists => use user.email
        // Adjust these lines to match your model.
        String email = resolveEmail(order);
        if (email == null || email.isBlank()) {
            log.warn("Skip notification: missing email. orderId={}", order.getId());
            return;
        }

        Map<String, Object> vars = Map.of(
                "orderId", order.getId().toString(),
                "totalPrice", order.getTotalPrice() == null ? null : order.getTotalPrice().toPlainString(),
                "paidAt", order.getPaidAt() == null ? null : order.getPaidAt().toString()
        );

        notificationService.enqueuePaymentSucceeded(order.getId(), email, vars);
    }

    private void enqueueRefundSucceeded(Order order, String chargeId) {
        String email = resolveEmail(order);
        if (email == null || email.isBlank()) {
            log.warn("Skip notification: missing email. orderId={}", order.getId());
            return;
        }

        Map<String, Object> vars = Map.of(
                "orderId", order.getId().toString(),
                "refundedAt", order.getRefundedAt() == null ? null : order.getRefundedAt().toString(),
                "refundedAmount", order.getRefundedAmount() == null ? null : order.getRefundedAmount().toPlainString(),
                "chargeId", chargeId
        );

        notificationService.enqueueRefundSucceeded(order.getId(), email, vars);
    }

    /**
     * Resolve recipient email for notifications.
     * Update this method based on your domain model (guest orders vs registered users).
     */
    private String resolveEmail(Order order) {
        if (order.getUser() != null && order.getUser().getEmail() != null) {
            return order.getUser().getEmail();
        }
        // If you have an email field on Order, use it here instead:
        // return order.getCustomerEmail();
        return null;
    }
}

