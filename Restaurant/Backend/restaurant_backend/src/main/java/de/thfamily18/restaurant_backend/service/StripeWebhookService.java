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
            // ✅ ONLY use refund events as source of truth
            "refund.created",
            "refund.updated"
            //"charge.refunded", // refund confirmed
            //"charge.refund.updated" // add, ✔ refund.updated → save each refund (history)
            //✔ charge.refunded → update aggregate amount (aggregate)
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
        // ===== ROUTING =====
        switch (type) {

            case "payment_intent.succeeded",
                 "payment_intent.payment_failed",
                 "payment_intent.canceled" -> handlePaymentIntentEvent(type, obj);

            // ✅ ONLY refund events
            case "refund.created",
                 "refund.updated" -> handleSingleRefund(obj);

            default -> log.debug("Unhandled event type={}", type);
        }
    }

    /**
     * Handle a PaymentIntent event.
     * "id" : "pi_3TK4KgPKeTTLfSwx10BF7eAV",
     *   "object" : "payment_intent",
     *   "amount" : 1100,
     *   "amount_capturable" : 0,
     *   "amount_details" : {
     *     "tip" : { }
     *   },
     *   "amount_received" : 1100,
     *   "application" : null,
     *   "application_fee_amount" : null,
     *   "automatic_payment_methods" : {
     *     "allow_redirects" : "never",
     *     "enabled" : true
     *   },
     *   "canceled_at" : null,
     *   "cancellation_reason" : null,
     *   "capture_method" : "automatic",
     *   "client_secret" : "pi_3TK4KgPKeTTLfSwx10BF7eAV_secret_UBIOv94IHzHrwbGS10YvuiFxe",
     *   "confirmation_method" : "automatic",
     *   "created" : 1775686342,
     *   "currency" : "eur",
     *   "customer" : null,
     *   "customer_account" : null,
     *   "description" : "Order fdd68809-c17a-4036-8626-cda54eaef31e",
     *   "excluded_payment_method_types" : null,
     *   "last_payment_error" : null,
     *   "latest_charge" : "ch_3TK4KgPKeTTLfSwx1XaSFAJK",
     *   "livemode" : false,
     *   "metadata" : {
     *     "orderId" : "fdd68809-c17a-4036-8626-cda54eaef31e"
     *   },
     *   "next_action" : null,
     *   "on_behalf_of" : null,
     *   "payment_method" : "pm_1TK4P1PKeTTLfSwxgZVDaYKl",
     *   "payment_method_configuration_details" : {
     *     "id" : "pmc_1SqHnIPKeTTLfSwx6ldss8Ly",
     *     "parent" : null
     *   },
     *   "payment_method_options" : {
     *     "card" : {
     *       "installments" : null,
     *       "mandate_options" : null,
     *       "network" : null,
     *       "request_three_d_secure" : "automatic"
     *     },
     *     "link" : {
     *       "persistent_token" : null
     *     }
     *   },
     *   "payment_method_types" : [ "card", "link" ],
     *   "processing" : null,
     *   "receipt_email" : null,
     *   "review" : null,
     *   "setup_future_usage" : null,
     *   "shipping" : null,
     *   "source" : null,
     *   "statement_descriptor" : null,
     *   "statement_descriptor_suffix" : null,
     *   "status" : "succeeded",
     *   "transfer_data" : null,
     *   "transfer_group" : null
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
                order.setUpdatedAt(LocalDateTime.now());

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
     *
     *--------
     {
     "id" : "re_3TK4KgPKeTTLfSwx1475I7m4",
     "object" : "refund",
     "amount" : 399,
     "balance_transaction" : "txn_3TK4KgPKeTTLfSwx1WUp3RRp",
     "charge" : "ch_3TK4KgPKeTTLfSwx1XaSFAJK",
     "created" : 1775687052,
     "currency" : "eur",
     "destination_details" : {
     "card" : {
     "reference" : "4255450042952669",
     "reference_status" : "available",
     "reference_type" : "acquirer_reference_number",
     "type" : "refund"
     },
     "type" : "card"
     },
     "metadata" : { },
     "payment_intent" : "pi_3TK4KgPKeTTLfSwx10BF7eAV",
     "reason" : null,
     "receipt_number" : null,
     "source_transfer_reversal" : null,
     "status" : "succeeded",
     "transfer_reversal" : null
     } type=charge.refund.updated

     ####
     "id" : "ch_3TK4KgPKeTTLfSwx1XaSFAJK",
     "object" : "charge",
     "amount" : 1100,
     "amount_captured" : 1100,
     "amount_refunded" : 399,
     "application" : null,
     "application_fee" : null,
     "application_fee_amount" : null,
     "balance_transaction" : "txn_3TK4KgPKeTTLfSwx1o1j3250",
     "billing_details" : {
     "address" : {
     "city" : null,
     "country" : "DE",
     "line1" : null,
     "line2" : null,
     "postal_code" : null,
     "state" : null
     },
     "email" : null,
     "name" : null,
     "phone" : null,
     "tax_id" : null
     },
     "calculated_statement_descriptor" : "Stripe",
     "captured" : true,
     "created" : 1775686611,
     "currency" : "eur",
     "customer" : null,
     "description" : "Order fdd68809-c17a-4036-8626-cda54eaef31e",
     "destination" : null,
     "dispute" : null,
     "disputed" : false,
     "failure_balance_transaction" : null,
     "failure_code" : null,
     "failure_message" : null,
     "fraud_details" : { },
     "livemode" : false,
     "metadata" : {
     "orderId" : "fdd68809-c17a-4036-8626-cda54eaef31e"
     },
     "on_behalf_of" : null,
     "order" : null,
     "outcome" : {
     "advice_code" : null,
     "network_advice_code" : null,
     "network_decline_code" : null,
     "network_status" : "approved_by_network",
     "reason" : null,
     "risk_level" : "normal",
     "risk_score" : 48,
     "seller_message" : "Payment complete.",
     "type" : "authorized"
     },
     "paid" : true,
     "payment_intent" : "pi_3TK4KgPKeTTLfSwx10BF7eAV",
     "payment_method" : "pm_1TK4P1PKeTTLfSwxgZVDaYKl",
     "payment_method_details" : {
     "card" : {
     "amount_authorized" : 1100,
     "authorization_code" : "456700",
     "brand" : "visa",
     "checks" : {
     "address_line1_check" : null,
     "address_postal_code_check" : null,
     "cvc_check" : "pass"
     },
     "country" : "US",
     "exp_month" : 2,
     "exp_year" : 2027,
     "extended_authorization" : {
     "status" : "disabled"
     },
     "fingerprint" : "xY8aPWmtq0LskwqL",
     "funding" : "credit",
     "incremental_authorization" : {
     "status" : "unavailable"
     },
     "installments" : null,
     "last4" : "4242",
     "mandate" : null,
     "multicapture" : {
     "status" : "unavailable"
     },
     "network" : "visa",
     "network_token" : {
     "used" : false
     },
     "network_transaction_id" : "120895697808710",
     "overcapture" : {
     "maximum_amount_capturable" : 1100,
     "status" : "unavailable"
     },
     "regulated_status" : "unregulated",
     "three_d_secure" : null,
     "wallet" : null
     },
     "type" : "card"
     },
     "radar_options" : { },
     "receipt_email" : null,
     "receipt_number" : null,
     "receipt_url" : "https://pay.stripe.com/receipts/payment/CAcaFwoVYWNjdF8xU3FIbWxQS2VUVExmU3d4KI2r284GMgZZEPB9rXw6LBaTcIXVf3wz5z5b4TpLrf-0AusxlbFCwfMASyKPYgIib7a288b3wvg44lpR",
     "refunded" : false,
     "review" : null,
     "shipping" : null,
     "source" : null,
     "source_transfer" : null,
     "statement_descriptor" : null,
     "statement_descriptor_suffix" : null,
     "status" : "succeeded",
     "transfer_data" : null,
     "transfer_group" : null
     }
     . type=charge.refunded
     }
     * @param obj
     */
//    private void handleRefundEvent(JsonNode obj) {
//
//        String paymentIntentId = obj.path("payment_intent").asText(null);
//        String chargeId = obj.path("id").asText(null);
//
//        if (paymentIntentId == null) {
//            log.warn("Missing payment_intent. chargeId={}", chargeId);
//            return;
//        }
//
//        Order order = orderRepo.findByStripePaymentIntentId(paymentIntentId).orElse(null);
//        if (order == null) {
//            log.warn("Missing order. chargeId={}, paymentIntentId={}", chargeId, paymentIntentId);
//            return;
//        }
//
//        JsonNode refundsNode = obj.path("refunds").path("data");
//        if (!refundsNode.isArray() || refundsNode.isEmpty()) {
//            log.warn("Missing refunds. chargeId={}, paymentIntentId={}", chargeId, paymentIntentId);
//            return;
//        }
//
//        // ===== 1. Extract IDs =====
//        List<String> refundIds = new ArrayList<>();
//        for (JsonNode r : refundsNode) {
//            refundIds.add(r.path("id").asText());
//        }
//
//        // ===== 2. Load existing (avoid N+1) =====
//        Map<String, Refund> existingMap = refundRepo
//                .findAllByStripeRefundIdIn(refundIds)
//                .stream()
//                .collect(Collectors.toMap(Refund::getStripeRefundId, r -> r));
//
//        List<Refund> toSave = new ArrayList<>();
//
//        // ===== 3. UPSERT =====
//        for (JsonNode r : refundsNode) {
//
//            String refundId = r.path("id").asText();
//            long amountCents = r.path("amount").asLong();
//            String statusRaw = r.path("status").asText();
//            String failureReason = r.path("failure_reason").asText(null);
//            String reason = r.path("reason").asText(null);
//
//            StripeRefundStatus status = StripeRefundStatus.fromStripe(statusRaw);
//
//            Refund entity = existingMap.get(refundId);
//
//            if (entity == null) {
//                entity = Refund.builder()
//                        .order(order)
//                        .stripeRefundId(refundId)
//                        .stripeChargeId(chargeId)
//                        .amount(BigDecimal.valueOf(amountCents, 2))
//                        .status(status)
//                        .reason(reason)
//                        .failureReason(failureReason)
//                        .createdAt(LocalDateTime.now())
//                        .build();
//            } else {
//                entity.setStatus(status);
//                entity.setFailureReason(failureReason);
//            }
//
//            toSave.add(entity);
//        }
//
//        // ===== 4. Batch save =====
//        refundRepo.saveAll(toSave);
//
//        // ===== 5. Recalculate aggregate (ONLY SUCCEEDED) =====
//        BigDecimal totalRefunded = refundRepo.sumSucceededAmountByOrderId(order.getId());
//        if (totalRefunded == null) totalRefunded = BigDecimal.ZERO;
//
//        order.setRefundedAmount(totalRefunded);
//
//        // ===== 6. Business logic =====
//        BigDecimal totalPrice = Objects.requireNonNullElse(
//                order.getTotalPrice(),
//                BigDecimal.ZERO
//        );
//
//        if (totalRefunded.compareTo(BigDecimal.ZERO) == 0) {
//
//            order.setRefundStatus(RefundStatus.REQUESTED);
//
//        } else if (totalRefunded.compareTo(totalPrice) >= 0) {
//
//            order.setRefundStatus(RefundStatus.REFUNDED);
//            order.setPaymentStatus(PaymentStatus.REFUNDED);
//            order.setRefundedAt(LocalDateTime.now());
//
//            registerAfterCommit(() -> enqueueRefundSucceeded(order, chargeId));
//
//        } else {
//
//            order.setRefundStatus(RefundStatus.PARTIAL);
//        }
//
//        orderRepo.save(order);
//
//        // ===== 7. Logging (audit-friendly) =====
//        log.info(
//                "Refund sync completed. orderId={}, refunded={}, refundCount={}, chargeId={}",
//                order.getId(),
//                totalRefunded,
//                toSave.size(),
//                chargeId
//        );
//    }

    private void handleSingleRefund(JsonNode obj) {

        String refundId = obj.path("id").asText();
        String paymentIntentId = obj.path("payment_intent").asText(null);
        String chargeId = obj.path("charge").asText(null);

        if (paymentIntentId == null) {
            log.warn("Missing payment_intent. refundId={}", refundId);
            return;
        }

        Order order = orderRepo.findByStripePaymentIntentId(paymentIntentId)
                .orElse(null);

        if (order == null) {
            log.warn("Missing order. refundId={}, pi={}", refundId, paymentIntentId);
            return;
        }

        long amountCents = obj.path("amount").asLong();
        String statusRaw = obj.path("status").asText();
        String reason = obj.path("reason").asText(null);
        String failureReason = obj.path("failure_reason").asText(null);

        StripeRefundStatus status = StripeRefundStatus.fromStripe(statusRaw);

        // 🔥 UPSERT (IDEMPOTENT)
        Refund refund = refundRepo.findByStripeRefundId(refundId).orElse(null);

        if (refund == null) {
            refund = Refund.builder()
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
            refund.setStatus(status);
            refund.setFailureReason(failureReason);
        }

        refundRepo.save(refund);

        // 🔥 SOURCE OF TRUTH = DB
        BigDecimal totalRefunded =
                Optional.ofNullable(refundRepo.sumSucceededAmountByOrderId(order.getId()))
                        .orElse(BigDecimal.ZERO);

        updateOrderState(order, totalRefunded, chargeId);
    }

    private void updateOrderState(Order order, BigDecimal totalRefunded, String chargeId) {

        order.setRefundedAmount(totalRefunded);

        BigDecimal totalPrice = Optional.ofNullable(order.getTotalPrice())
                .orElse(BigDecimal.ZERO);

        if (totalRefunded.compareTo(BigDecimal.ZERO) == 0) {

            order.setRefundStatus(RefundStatus.REQUESTED);

        } else if (totalRefunded.compareTo(totalPrice) >= 0) {

            if (order.getPaymentStatus() != PaymentStatus.REFUNDED) {

                order.setPaymentStatus(PaymentStatus.REFUNDED);
                order.setRefundStatus(RefundStatus.REFUNDED);
                order.setRefundedAt(LocalDateTime.now());

                registerAfterCommit(() ->
                        enqueueRefundSucceeded(order, chargeId)
                );
            }

        } else {
            order.setRefundStatus(RefundStatus.PARTIAL);
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);
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



