package de.thfamily18.restaurant_backend.service;

import com.stripe.model.Event;
import com.stripe.net.Webhook;
import de.thfamily18.restaurant_backend.entity.Order;
import de.thfamily18.restaurant_backend.entity.PaymentStatus;
import de.thfamily18.restaurant_backend.entity.Refund;
import de.thfamily18.restaurant_backend.entity.RefundProviderStatus;
import de.thfamily18.restaurant_backend.entity.RefundStatus;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookService {

    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            "payment_intent.succeeded",
            "payment_intent.processing",
            "payment_intent.payment_failed",
            "payment_intent.canceled",

            // Refund events are the source of truth for refund persistence.
            "refund.created",
            "refund.updated"
    );

    private final OrderRepository orderRepo;
    private final RefundRepository refundRepo;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Value("${stripe.webhookSecret}")
    private String webhookSecret;

    @Transactional
    public void handle(
            String payload,
            String sigHeader
    ) {

        // ===== 1. Validate webhook secret =====
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException(
                    "Missing stripe.webhookSecret (STRIPE_WEBHOOK_SECRET)"
            );
        }

        // ===== 2. Verify Stripe signature =====
        final Event event;

        try {
            event = Webhook.constructEvent(
                    payload,
                    sigHeader,
                    webhookSecret
            );
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Invalid Stripe signature",
                    ex
            );
        }

        String type = event.getType();

        // Ignore Stripe events that this backend does not use.
        if (!SUPPORTED_EVENTS.contains(type)) {
            return;
        }

        // ===== 3. Parse webhook data.object =====
        final JsonNode obj;

        try {
            obj = objectMapper
                    .readTree(payload)
                    .path("data")
                    .path("object");

            log.debug(
                    "RAW Stripe webhook object={}",
                    obj.toPrettyString()
            );

        } catch (Exception ex) {

            log.warn(
                    "Cannot parse Stripe webhook payload. eventId={}, type={}",
                    event.getId(),
                    type,
                    ex
            );

            /*
             * The payload was already signature-verified but cannot be parsed
             * by our business handler. Retrying the exact same payload will
             * not fix the parsing problem.
             */
            return;
        }

        log.info(
                "Stripe webhook received. eventId={}, type={}",
                event.getId(),
                type
        );

        // ===== 4. Route event =====
        switch (type) {

            case "payment_intent.succeeded",
                 "payment_intent.processing",
                 "payment_intent.payment_failed",
                 "payment_intent.canceled"
                    -> handlePaymentIntentEvent(type, obj);

            case "refund.created",
                 "refund.updated"
                    -> handleSingleRefund(obj);

            default ->
                    log.debug(
                            "Unhandled Stripe event type={}",
                            type
                    );
        }
    }

    // ========================================================================
    // PAYMENT INTENT
    // ========================================================================

    private void handlePaymentIntentEvent(
            String type,
            JsonNode obj
    ) {

        final String piId =
                obj.path("id")
                        .asText(null);

        final String orderIdStr =
                obj.path("metadata")
                        .path("orderId")
                        .asText(null);

        log.info(
                "Stripe PaymentIntent event received. type={}, piId={}, orderId={}",
                type,
                piId,
                orderIdStr
        );

        // ===== 1. Validate metadata =====
        if (orderIdStr == null || orderIdStr.isBlank()) {

            log.warn(
                    "Missing metadata.orderId. type={}, piId={}",
                    type,
                    piId
            );

            return;
        }

        final UUID orderId;

        try {
            orderId = UUID.fromString(orderIdStr);

        } catch (IllegalArgumentException ex) {

            log.warn(
                    "Invalid orderId UUID in Stripe metadata. orderId={}, piId={}",
                    orderIdStr,
                    piId
            );

            return;
        }

        // ===== 2. Load Order =====
        Order order = orderRepo.findById(orderId)
                .orElse(null);

        if (order == null) {

            /*
             * This is a business-data mismatch, not a temporary DB outage.
             * Retrying the same webhook will not create the missing order.
             */
            log.warn(
                    "Stripe webhook references unknown order. orderId={}, piId={}, type={}",
                    orderId,
                    piId,
                    type
            );

            return;
        }

        // ===== 3. PaymentIntent integrity check =====
        if (order.getStripePaymentIntentId() != null
                && !order.getStripePaymentIntentId().isBlank()
                && piId != null
                && !piId.isBlank()
                && !order.getStripePaymentIntentId().equals(piId)) {

            log.error(
                    "PaymentIntent mismatch. orderId={}, expectedPiId={}, webhookPiId={}, type={}",
                    orderId,
                    order.getStripePaymentIntentId(),
                    piId,
                    type
            );

            /*
             * Never allow an event belonging to another PaymentIntent
             * to overwrite the PaymentIntent already bound to this Order.
             */
            return;
        }

        // ===== 4. State guards / idempotency =====

        // A fully refunded payment must never be downgraded.
        if (order.getPaymentStatus() == PaymentStatus.REFUNDED) {

            log.info(
                    "Ignore PaymentIntent event because order is already REFUNDED. orderId={}, type={}, piId={}",
                    orderId,
                    type,
                    piId
            );

            return;
        }

        /*
         * A successful payment must not be downgraded by late processing,
         * failed, or canceled events.
         */
        if (order.getPaymentStatus() == PaymentStatus.PAID
                && (
                "payment_intent.processing".equals(type)
                        || "payment_intent.payment_failed".equals(type)
                        || "payment_intent.canceled".equals(type)
        )) {

            log.info(
                    "Ignore late PaymentIntent downgrade. orderId={}, type={}, piId={}",
                    orderId,
                    type,
                    piId
            );

            return;
        }

        // Duplicate payment success.
        if (order.getPaymentStatus() == PaymentStatus.PAID
                && "payment_intent.succeeded".equals(type)) {

            log.info(
                    "Ignore duplicate payment success. orderId={}, piId={}",
                    orderId,
                    piId
            );

            return;
        }

        // ===== 5. State transition =====
        switch (type) {

            case "payment_intent.succeeded" -> {

                order.setPaymentStatus(
                        PaymentStatus.PAID
                );

                order.setPaidAt(
                        LocalDateTime.now()
                );

                order.setStripePaymentIntentId(
                        piId
                );

                /*
                 * Notification will only run if this transaction
                 * successfully commits.
                 */
                registerAfterCommit(
                        () -> enqueuePaymentSucceeded(order)
                );
            }

            case "payment_intent.processing" -> {

                order.setPaymentStatus(
                        PaymentStatus.PROCESSING
                );

                order.setStripePaymentIntentId(
                        piId
                );
            }

            case "payment_intent.payment_failed" -> {

                order.setPaymentStatus(
                        PaymentStatus.FAILED
                );

                order.setStripePaymentIntentId(
                        piId
                );
            }

            case "payment_intent.canceled" -> {

                order.setPaymentStatus(
                        PaymentStatus.CANCELED
                );

                order.setStripePaymentIntentId(
                        piId
                );
            }

            default -> {

                log.debug(
                        "Unhandled PaymentIntent event type={}",
                        type
                );

                return;
            }
        }

        // ===== 6. Persist =====
        orderRepo.save(order);

        log.info(
                "Order payment state updated. orderId={}, status={}, piId={}",
                orderId,
                order.getPaymentStatus(),
                piId
        );
    }

    // ========================================================================
    // REFUND
    // ========================================================================

    private void handleSingleRefund(JsonNode obj) {

        // ===== 1. Extract Stripe data =====

        String refundId =
                obj.path("id")
                        .asText(null);

        String paymentIntentId =
                obj.path("payment_intent")
                        .asText(null);

        String chargeId =
                obj.path("charge")
                        .asText(null);

        if (refundId == null || refundId.isBlank()) {

            log.warn(
                    "Stripe refund webhook missing refund id"
            );

            return;
        }

        if (paymentIntentId == null
                || paymentIntentId.isBlank()) {

            log.warn(
                    "Stripe refund webhook missing payment_intent. refundId={}",
                    refundId
            );

            return;
        }

        // ===== 2. Resolve Order =====
        Order order =
                orderRepo.findByStripePaymentIntentId(
                        paymentIntentId
                ).orElse(null);

        if (order == null) {

            log.warn(
                    "Order not found for Stripe refund. refundId={}, paymentIntentId={}",
                    refundId,
                    paymentIntentId
            );

            return;
        }

        // ===== 3. Validate refund amount =====
        long amountCents =
                obj.path("amount")
                        .asLong(0);

        if (amountCents <= 0) {

            log.warn(
                    "Invalid Stripe refund amount. refundId={}, amountCents={}",
                    refundId,
                    amountCents
            );

            return;
        }

        // ===== 4. Parse refund status =====
        String statusRaw =
                obj.path("status")
                        .asText(null);

        if (statusRaw == null || statusRaw.isBlank()) {

            log.warn(
                    "Stripe refund webhook missing status. refundId={}",
                    refundId
            );

            return;
        }

        String reason =
                obj.path("reason")
                        .asText(null);

        String failureReason =
                obj.path("failure_reason")
                        .asText(null);

        final RefundProviderStatus providerStatus;

        try {

            providerStatus =
                    RefundProviderStatus.fromProvider(
                            statusRaw
                    );

        } catch (IllegalArgumentException ex) {

            log.warn(
                    "Unsupported Stripe refund status. refundId={}, status={}",
                    refundId,
                    statusRaw
            );

            return;
        }

        BigDecimal refundAmount =
                BigDecimal.valueOf(
                        amountCents,
                        2
                );

        // ===== 5. UPSERT Refund =====

        /*
         * stripeRefundId is the provider-level idempotency identity.
         *
         * refund.created:
         *      INSERT if refund does not exist.
         *
         * refund.updated:
         *      UPDATE the same refund lifecycle.
         */
        Refund refund =
                refundRepo.findByStripeRefundId(
                        refundId
                ).orElse(null);

        if (refund == null) {

            refund = Refund.builder()
                    .order(order)
                    .stripeRefundId(refundId)
                    .stripeChargeId(chargeId)
                    .amount(refundAmount)
                    .status(providerStatus)
                    .reason(reason)
                    .failureReason(failureReason)
                    .createdAt(LocalDateTime.now())
                    .build();

            log.info(
                    "Creating Stripe refund record. orderId={}, refundId={}, status={}, amount={}",
                    order.getId(),
                    refundId,
                    providerStatus,
                    refundAmount
            );

        } else {

            refund.setStatus(
                    providerStatus
            );

            refund.setFailureReason(
                    failureReason
            );

            if (chargeId != null
                    && !chargeId.isBlank()) {

                refund.setStripeChargeId(
                        chargeId
                );
            }

            if (reason != null
                    && !reason.isBlank()) {

                refund.setReason(
                        reason
                );
            }

            log.info(
                    "Updating Stripe refund record. orderId={}, refundId={}, status={}",
                    order.getId(),
                    refundId,
                    providerStatus
            );
        }

        /*
         * THIS is the authoritative place where a Stripe refund
         * is persisted to the database.
         */
        refundRepo.save(refund);

        // ===== 6. Recalculate aggregate from DB =====

        /*
         * Only SUCCEEDED refunds count toward refundedAmount.
         *
         * Never trust Stripe charge.amount_refunded as our local aggregate.
         * The refunds table is our local source of truth.
         */
        BigDecimal totalRefunded =
                Optional.ofNullable(
                        refundRepo.sumSucceededAmountByOrderId(
                                order.getId()
                        )
                ).orElse(BigDecimal.ZERO);

        // ===== 7. Update Order aggregate =====
        updateOrderState(
                order,
                totalRefunded,
                providerStatus,
                chargeId
        );

        log.info(
                "Stripe refund synchronization completed. orderId={}, refundId={}, providerStatus={}, refundedTotal={}",
                order.getId(),
                refundId,
                providerStatus,
                totalRefunded
        );
    }

    // ========================================================================
    // ORDER REFUND STATE
    // ========================================================================

    private void updateOrderState(
            Order order,
            BigDecimal totalRefunded,
            RefundProviderStatus currentRefundStatus,
            String chargeId
    ) {

        order.setRefundedAmount(
                totalRefunded
        );

        BigDecimal totalPrice =
                Optional.ofNullable(
                        order.getTotalPrice()
                ).orElse(BigDecimal.ZERO);

        boolean wasFullyRefunded =
                order.getPaymentStatus()
                        == PaymentStatus.REFUNDED;

        // ===== 1. Fully refunded =====
        if (totalPrice.compareTo(BigDecimal.ZERO) > 0
                && totalRefunded.compareTo(totalPrice) >= 0) {

            order.setRefundStatus(
                    RefundStatus.REFUNDED
            );

            order.setPaymentStatus(
                    PaymentStatus.REFUNDED
            );

            if (order.getRefundedAt() == null) {

                order.setRefundedAt(
                        LocalDateTime.now()
                );
            }

            /*
             * Send full-refund notification only when the Order transitions
             * into REFUNDED for the first time.
             */
            if (!wasFullyRefunded) {

                registerAfterCommit(
                        () -> enqueueRefundSucceeded(
                                order,
                                chargeId
                        )
                );
            }
        }

        // ===== 2. Partial refund =====
        else if (totalRefunded.compareTo(
                BigDecimal.ZERO
        ) > 0) {

            order.setRefundStatus(
                    RefundStatus.PARTIAL
            );

            /*
             * Partial refunds do not change the payment to REFUNDED.
             * The original payment remains successfully PAID.
             */
            order.setPaymentStatus(
                    PaymentStatus.PAID
            );

            order.setRefundedAt(null);
        }

        // ===== 3. Refund submitted but not completed =====
        else if (
                currentRefundStatus == RefundProviderStatus.PENDING
                        || currentRefundStatus
                        == RefundProviderStatus.REQUIRES_ACTION
        ) {

            order.setRefundStatus(
                    RefundStatus.REQUESTED
            );
        }

        // ===== 4. No successful refunds =====
        else {

            /*
             * Example:
             * - refund FAILED
             * - refund CANCELED
             * - no previous successful refund exists
             */
            order.setRefundStatus(
                    RefundStatus.NOT_REQUESTED
            );
        }

        orderRepo.save(order);
    }

    // ========================================================================
    // NOTIFICATIONS
    // ========================================================================

    private void enqueuePaymentSucceeded(
            Order order
    ) {

        String email =
                resolveEmail(order);

        if (email == null || email.isBlank()) {

            log.warn(
                    "Skip payment notification: missing email. orderId={}",
                    order.getId()
            );

            return;
        }

        /*
         * Do not use Map.of(...) here because Map.of does not allow null
         * keys or values.
         */
        Map<String, Object> vars =
                new HashMap<>();

        vars.put(
                "orderId",
                order.getId().toString()
        );

        if (order.getTotalPrice() != null) {

            vars.put(
                    "totalPrice",
                    order.getTotalPrice()
                            .toPlainString()
            );
        }

        if (order.getPaidAt() != null) {

            vars.put(
                    "paidAt",
                    order.getPaidAt()
                            .toString()
            );
        }

        notificationService.enqueuePaymentSucceeded(
                order.getId(),
                email,
                vars
        );
    }

    private void enqueueRefundSucceeded(
            Order order,
            String chargeId
    ) {

        String email =
                resolveEmail(order);

        if (email == null || email.isBlank()) {

            log.warn(
                    "Skip refund notification: missing email. orderId={}",
                    order.getId()
            );

            return;
        }

        Map<String, Object> vars =
                new HashMap<>();

        vars.put(
                "orderId",
                order.getId().toString()
        );

        if (order.getRefundedAt() != null) {

            vars.put(
                    "refundedAt",
                    order.getRefundedAt()
                            .toString()
            );
        }

        if (order.getRefundedAmount() != null) {

            vars.put(
                    "refundedAmount",
                    order.getRefundedAmount()
                            .toPlainString()
            );
        }

        if (chargeId != null && !chargeId.isBlank()) {

            vars.put(
                    "chargeId",
                    chargeId
            );
        }

        notificationService.enqueueRefundSucceeded(
                order.getId(),
                email,
                vars
        );
    }

    // ========================================================================
    // EMAIL RESOLUTION
    // ========================================================================

    private String resolveEmail(
            Order order
    ) {

        if (order.getUser() != null
                && order.getUser().getEmail() != null
                && !order.getUser().getEmail().isBlank()) {

            return order.getUser()
                    .getEmail();
        }

        /*
         * Guest checkout:
         *
         * If Order has a customerEmail field, use it here:
         *
         * return order.getCustomerEmail();
         */

        return null;
    }

    // ========================================================================
    // TRANSACTION CALLBACK
    // ========================================================================

    private void registerAfterCommit(
            Runnable action
    ) {

        if (TransactionSynchronizationManager
                .isActualTransactionActive()) {

            TransactionSynchronizationManager
                    .registerSynchronization(
                            new TransactionSynchronization() {

                                @Override
                                public void afterCommit() {
                                    action.run();
                                }
                            }
                    );

        } else {

            /*
             * Fallback for tests or calls executed outside a transaction.
             */
            action.run();
        }
    }
}