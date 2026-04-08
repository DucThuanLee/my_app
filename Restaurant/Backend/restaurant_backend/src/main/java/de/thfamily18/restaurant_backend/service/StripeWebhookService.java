package de.thfamily18.restaurant_backend.service;

import com.stripe.model.Event;
import com.stripe.net.Webhook;
import de.thfamily18.restaurant_backend.entity.*;
import de.thfamily18.restaurant_backend.exception.ResourceNotFoundException;
import de.thfamily18.restaurant_backend.notification.NotificationService;
import de.thfamily18.restaurant_backend.repository.OrderRepository;
import de.thfamily18.restaurant_backend.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookService {

    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            "payment_intent.succeeded",
            "payment_intent.payment_failed",
            "payment_intent.canceled",
            "charge.refunded", // refund confirmed
            "charge.refund.updated" // add
    );

    private final OrderRepository orderRepo;
    private final RefundRepository refundRepo;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Value("${stripe.webhookSecret}")
    private String webhookSecret;

    @Transactional
    public void handle(String payload, String sigHeader) {
        // Fail-fast if missing secret (signature verification must be enabled in production).
        // ===== 1. Verify Stripe signature =====
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
            log.info("RAW webhook obj = {}", obj.toPrettyString());
        } catch (Exception e) {
            log.warn("Cannot parse webhook payload. eventId={}, type={}", event.getId(), type, e);
            // Return 200 OK to prevent Stripe retry storms; controller should always respond OK.
            return;
        }

        log.info("Stripe webhook received. type={}, eventId={}", type, event.getId());

        // ===== 2. Route event =====
        if (type.startsWith("payment_intent.")) {
            handlePaymentIntentEvent(type, obj);
            return;
        }

        if ("charge.refunded".equals(type) || "charge.refund.updated".equals(type)) {
            handleRefundEvent(obj);
        }
    }

    /**
     * Handle a PaymentIntent event.
     * {
     *   "id": "evt_1Nx...",
     *   "type": "payment_intent.succeeded",
     *   "data": {
     *     "object": {
     *       "id": "pi_3O...",
     *       "amount": 2000, // Amount (e.g., 20.00 USD - smallest unit)
     *       "currency": "usd",
     *       "status": "succeeded",
     *       "metadata": {
     *         "order_id": "6BAF-123", // It's very important that you map to your database.
     *         "user_id": "user_88"
     *       },
     *       "payment_method": "pm_1Nx...",
     *       "receipt_email": "customer@example.com"
     *     }
     *   }
     * }
     * @param type
     * @param obj
     */
    private void handlePaymentIntentEvent(String type, JsonNode obj) {

        final String piId = obj.path("id").asText(null);
        final String orderIdStr = obj.path("metadata").path("orderId").asText(null);

        log.info("PI event received. type={}, piId={}, orderId={}", type, piId, orderIdStr);

        // ===== 1. Validate metadata =====
        if (orderIdStr == null || orderIdStr.isBlank()) {
            log.warn("Missing metadata.orderId, piId={}", piId);
            return;
        }

        final UUID orderId;
        try {
            orderId = UUID.fromString(orderIdStr);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid orderId UUID in metadata. orderId={}, piId={}", orderIdStr, piId);
            return;
        }

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        // ===== 2. Idempotency / state guards =====

        // Already refunded → ignore everything
        if (order.getPaymentStatus() == PaymentStatus.REFUNDED) {
            log.info("Ignore PI event: already REFUNDED. orderId={}, type={}, piId={}", orderId, type, piId);
            return;
        }

        // Already paid → ignore downgrade events
        if (order.getPaymentStatus() == PaymentStatus.PAID &&
                ("payment_intent.payment_failed".equals(type)
                        || "payment_intent.canceled".equals(type))) {

            log.info("Ignore late downgrade event. orderId={}, type={}, piId={}", orderId, type, piId);
            return;
        }

        // Duplicate success
        if (order.getPaymentStatus() == PaymentStatus.PAID &&
                "payment_intent.succeeded".equals(type)) {

            log.info("Ignore duplicate success event. orderId={}, piId={}", orderId, piId);
            return;
        }

        // ===== 3. Fraud / mismatch detection =====
        if (order.getStripePaymentIntentId() != null
                && !order.getStripePaymentIntentId().isBlank()
                && piId != null
                && !piId.isBlank()
                && !order.getStripePaymentIntentId().equals(piId)) {

            log.warn("PaymentIntent mismatch detected! orderId={}, dbPiId={}, webhookPiId={}",
                    orderId, order.getStripePaymentIntentId(), piId);

            // 👉 Optional: you can decide to reject update here
            // return;
        }

        // ===== 4. State transition =====
        switch (type) {

            case "payment_intent.succeeded" -> {
                order.setPaymentStatus(PaymentStatus.PAID);
                order.setPaidAt(LocalDateTime.now());
                order.setStripePaymentIntentId(piId);

                // ✅ IMPORTANT: enqueue AFTER COMMIT
                registerAfterCommit(() -> enqueuePaymentSucceeded(order));
            }

            case "payment_intent.processing" -> {
                // Optional but recommended
                order.setPaymentStatus(PaymentStatus.PROCESSING);
                order.setStripePaymentIntentId(piId);
            }

            case "payment_intent.payment_failed" -> {
                order.setPaymentStatus(PaymentStatus.FAILED);
                order.setStripePaymentIntentId(piId);
            }

            case "payment_intent.canceled" -> {
                order.setPaymentStatus(PaymentStatus.CANCELED);
                order.setStripePaymentIntentId(piId);
            }

            default -> {
                log.debug("Unhandled PI event type: {}", type);
                return;
            }
        }

        // ===== 5. Persist =====
        orderRepo.save(order);

        log.info("Order updated. orderId={}, status={}, piId={}",
                orderId, order.getPaymentStatus(), piId);
    }

    /**
     * Handle a Refund event.
     * {
     *   "id": "evt_1Ny...",
     *   "type": "charge.refunded",
     *   "data": {
     *     "object": {
     *       "id": "ch_3O...", // Original Charge ID
     *       "amount_refunded": 2000,
     *       "refunds": {
     *         "data": [
     *           {
     *             "id": "re_3O...", // This is the stripe_refund_id you need to save to the refunds table.
     *             "amount": 2000,
     *             "status": "succeeded",
     *             "payment_intent": "pi_3O...",
     *             "reason": "requested_by_customer"
     *           }
     *         ]
     *       },
     *       "status": "succeeded"
     *     }
     *   }
     * }
     * @param obj
     */
    private void handleRefundEvent(JsonNode obj) {

        String paymentIntentId = obj.path("payment_intent").asText(null);
        String chargeId = obj.path("id").asText(null);

        if (paymentIntentId == null) {
            log.warn("Missing payment_intent. chargeId={}", chargeId);
            return;
        }

        Order order = orderRepo.findByStripePaymentIntentId(paymentIntentId).orElse(null);
        if (order == null) {
            log.warn("Missing order. chargeId={}, paymentIntentId={}", chargeId, paymentIntentId);
            return;
        }

        JsonNode refundsNode = obj.path("refunds").path("data");
        if (!refundsNode.isArray() || refundsNode.isEmpty()) {
            log.warn("Missing refunds. chargeId={}, paymentIntentId={}", chargeId, paymentIntentId);
            return;
        }

        // ===== 1. Extract IDs =====
        List<String> refundIds = new ArrayList<>();
        for (JsonNode r : refundsNode) {
            refundIds.add(r.path("id").asText());
        }

        // ===== 2. Load existing (avoid N+1) =====
        Map<String, Refund> existingMap = refundRepo
                .findAllByStripeRefundIdIn(refundIds)
                .stream()
                .collect(Collectors.toMap(Refund::getStripeRefundId, r -> r));

        List<Refund> toSave = new ArrayList<>();

        // ===== 3. UPSERT =====
        for (JsonNode r : refundsNode) {

            String refundId = r.path("id").asText();
            long amountCents = r.path("amount").asLong();
            String statusRaw = r.path("status").asText();
            String failureReason = r.path("failure_reason").asText(null);
            String reason = r.path("reason").asText(null);

            StripeRefundStatus status = StripeRefundStatus.fromStripe(statusRaw);

            Refund entity = existingMap.get(refundId);

            if (entity == null) {
                entity = Refund.builder()
                        .order(order)
                        .stripeRefundId(refundId)
                        .stripeChargeId(chargeId)
                        .amount(BigDecimal.valueOf(amountCents, 2))
                        .status(status)
                        .reason(reason)
                        .failureReason(failureReason)
                        .createdAt(LocalDateTime.now())
                        .build();
            } else {
                entity.setStatus(status);
                entity.setFailureReason(failureReason);
            }

            toSave.add(entity);
        }

        // ===== 4. Batch save =====
        refundRepo.saveAll(toSave);

        // ===== 5. Recalculate aggregate (ONLY SUCCEEDED) =====
        BigDecimal totalRefunded = refundRepo.sumSucceededAmountByOrderId(order.getId());
        if (totalRefunded == null) totalRefunded = BigDecimal.ZERO;

        order.setRefundedAmount(totalRefunded);

        // ===== 6. Business logic =====
        BigDecimal totalPrice = Objects.requireNonNullElse(
                order.getTotalPrice(),
                BigDecimal.ZERO
        );

        if (totalRefunded.compareTo(BigDecimal.ZERO) == 0) {

            order.setRefundStatus(RefundStatus.REQUESTED);

        } else if (totalRefunded.compareTo(totalPrice) >= 0) {

            order.setRefundStatus(RefundStatus.REFUNDED);
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            order.setRefundedAt(LocalDateTime.now());

            registerAfterCommit(() -> enqueueRefundSucceeded(order, chargeId));

        } else {

            order.setRefundStatus(RefundStatus.PARTIAL);
        }

        orderRepo.save(order);

        // ===== 7. Logging (audit-friendly) =====
        log.info(
                "Refund sync completed. orderId={}, refunded={}, refundCount={}, chargeId={}",
                order.getId(),
                totalRefunded,
                toSave.size(),
                chargeId
        );
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

    private void registerAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}

