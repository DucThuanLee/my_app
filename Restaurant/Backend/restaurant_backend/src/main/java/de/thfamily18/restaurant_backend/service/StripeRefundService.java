package de.thfamily18.restaurant_backend.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;
import de.thfamily18.restaurant_backend.dto.payment.RefundResponse;
import de.thfamily18.restaurant_backend.entity.Order;
import de.thfamily18.restaurant_backend.entity.PaymentStatus;
import de.thfamily18.restaurant_backend.entity.RefundProviderStatus;
import de.thfamily18.restaurant_backend.exception.ResourceNotFoundException;
import de.thfamily18.restaurant_backend.repository.OrderRepository;
import de.thfamily18.restaurant_backend.service.payment.StripeGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeRefundService {

    private final StripeGateway stripeGateway;
    private final OrderRepository orderRepo;

    @Value("${stripe.secretKey:}")
    private String stripeSecretKey;

    public RefundResponse refundOrder(
            UUID orderId,
            UUID requestId,
            BigDecimal amount,
            String reason
    ) throws StripeException {

        // ===== 1. Load order =====
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found: " + orderId
                        )
                );

        // ===== 2. Validate payment =====
        if (order.getStripePaymentIntentId() == null
                || order.getStripePaymentIntentId().isBlank()) {

            throw new IllegalArgumentException(
                    "Order has no Stripe payment intent"
            );
        }

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalArgumentException(
                    "Only PAID orders can be refunded"
            );
        }

        if (order.getTotalPrice() == null
                || order.getTotalPrice().compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalStateException(
                    "Order has invalid total price"
            );
        }

        // ===== 3. Calculate refundable amount =====
        BigDecimal alreadyRefunded =
                order.getRefundedAmount() == null
                        ? BigDecimal.ZERO
                        : order.getRefundedAmount();

        BigDecimal remaining =
                order.getTotalPrice().subtract(alreadyRefunded);

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                    "Order has no refundable amount remaining"
            );
        }

        BigDecimal refundAmount =
                amount == null ? remaining : amount;

        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Refund amount must be greater than 0"
            );
        }

        if (refundAmount.compareTo(remaining) > 0) {
            throw new IllegalArgumentException(
                    "Refund amount exceeds remaining refundable amount"
            );
        }

        // ===== 4. Build Stripe refund request =====
        RefundCreateParams.Builder builder =
                RefundCreateParams.builder()
                        .setPaymentIntent(
                                order.getStripePaymentIntentId()
                        )
                        .setAmount(
                                toCents(refundAmount)
                        );

        RefundCreateParams.Reason stripeReason =
                mapReason(reason);

        if (stripeReason != null) {
            builder.setReason(stripeReason);
        }

        RefundCreateParams params = builder.build();

        // ===== 5. Stable idempotency key =====
        String idempotencyKey =
                "refund:"
                        + order.getId()
                        + ":"
                        + requestId;

        // ===== 6. Call Stripe =====
        Refund refund = stripeGateway.createRefund(
                params,
                requestOptions(idempotencyKey)
        );

        /*
         * Do not persist the Refund here.
         *
         * Stripe webhook is the source of truth and is responsible for
         * upserting the refund and updating the Order refund aggregate.
         *
         * This avoids racing the HTTP request transaction against Stripe
         * webhook delivery.
         */
        log.info(
                "Stripe refund requested. orderId={}, refundId={}, requestId={}, amount={}, status={}",
                orderId,
                refund.getId(),
                requestId,
                refundAmount,
                refund.getStatus()
        );

        // ===== 7. Response =====
        return new RefundResponse(
                order.getId(),
                order.getStripePaymentIntentId(),
                refund.getId(),
                RefundProviderStatus.PENDING,
                order.getPaymentStatus(),
                refundAmount,
                LocalDateTime.now()
        );
    }

    private long toCents(BigDecimal amount) {
        return amount.movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private RefundCreateParams.Reason mapReason(String reason) {

        if (reason == null || reason.isBlank()) {
            return null;
        }

        return switch (reason.toLowerCase(Locale.ROOT)) {

            case "duplicate" ->
                    RefundCreateParams.Reason.DUPLICATE;

            case "fraudulent" ->
                    RefundCreateParams.Reason.FRAUDULENT;

            case "requested_by_customer" ->
                    RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER;

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported refund reason: " + reason
                    );
        };
    }

    private RequestOptions requestOptions(
            String idempotencyKey
    ) {

        RequestOptions.RequestOptionsBuilder builder =
                RequestOptions.builder();

        if (stripeSecretKey != null
                && !stripeSecretKey.isBlank()) {

            builder.setApiKey(stripeSecretKey);
        }

        if (idempotencyKey != null
                && !idempotencyKey.isBlank()) {

            builder.setIdempotencyKey(idempotencyKey);
        }

        return builder.build();
    }
}