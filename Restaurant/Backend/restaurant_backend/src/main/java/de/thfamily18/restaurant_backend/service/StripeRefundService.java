package de.thfamily18.restaurant_backend.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;
import de.thfamily18.restaurant_backend.dto.payment.RefundResponse;
import de.thfamily18.restaurant_backend.entity.Order;
import de.thfamily18.restaurant_backend.entity.PaymentStatus;
import de.thfamily18.restaurant_backend.entity.RefundStatus;
import de.thfamily18.restaurant_backend.entity.StripeRefundStatus;
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
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeRefundService {

    private final StripeGateway stripeGateway;
    private final OrderRepository orderRepo;

    @Value("${stripe.secretKey:}")
    private String stripeSecretKey;

    @Transactional
    public RefundResponse refundOrder(UUID orderId, BigDecimal amount, String reason)
            throws StripeException {

        // ===== 1. Load order =====
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        // ===== 2. Validate business rules =====
        if (order.getStripePaymentIntentId() == null || order.getStripePaymentIntentId().isBlank()) {
            throw new IllegalArgumentException("Order has no Stripe payment intent");
        }

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalArgumentException("Only PAID orders can be refunded");
        }

        // ===== 3. Handle partial refund amount =====
        Long amountInCents = null;

        if (amount != null) {
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Refund amount must be greater than 0");
            }

            if (order.getTotalPrice() != null && amount.compareTo(order.getTotalPrice()) > 0) {
                throw new IllegalArgumentException("Refund amount exceeds order total");
            }

            amountInCents = toCents(amount);
        }

        // ===== 4. Build Stripe refund request =====
        RefundCreateParams.Builder builder = RefundCreateParams.builder()
                .setPaymentIntent(order.getStripePaymentIntentId());

        if (amountInCents != null) {
            builder.setAmount(amountInCents); // partial refund
        }

        if (reason != null && !reason.isBlank()) {
            builder.setReason(mapReason(reason));
        }

        RefundCreateParams params = builder.build();

        // ===== 5. Call Stripe with idempotency =====
        String idempotencyKey = "refund:" + order.getId();

        Refund refund = stripeGateway.createRefund(
                params,
                requestOptions(idempotencyKey)
        );

        // ===== 6. Update order state (request only, not final) =====
        order.setRefundRequestedAt(LocalDateTime.now());
        order.setRefundStatus(RefundStatus.REQUESTED);
        order.setStripeRefundStatus(StripeRefundStatus.PENDING);

        // NOTE:
        // Do NOT mark as REFUNDED here.
        // Webhook (charge.refunded / refund.updated) is the source of truth.

        orderRepo.save(order);

        log.info("Refund requested. orderId={}, refundId={}, status={}",
                orderId, refund.getId(), refund.getStatus());

        // ===== 7. Build response =====
        // Use requested amount if provided, otherwise fallback to current refundedAmount (may be null before webhook)
        return new RefundResponse(
                order.getId(),
                order.getStripePaymentIntentId(),
                refund.getId(),
                StripeRefundStatus.fromStripe(refund.getStatus()),
                order.getPaymentStatus(),
                order.getRefundedAmount() != null ? order.getRefundedAmount() : amount,
                order.getRefundRequestedAt()
        );
    }

    // ===== helper =====

    private long toCents(BigDecimal eur) {
        return eur.movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private RefundCreateParams.Reason mapReason(String reason) {
        return switch (reason.toLowerCase()) {
            case "duplicate" -> RefundCreateParams.Reason.DUPLICATE;
            case "fraudulent" -> RefundCreateParams.Reason.FRAUDULENT;
            case "requested_by_customer" -> RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER;
            default -> null;
        };
    }

    private RequestOptions requestOptions(String idempotencyKey) {
        RequestOptions.RequestOptionsBuilder b = RequestOptions.builder();

        if (stripeSecretKey != null && !stripeSecretKey.isBlank()) {
            b.setApiKey(stripeSecretKey);
        }

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            b.setIdempotencyKey(idempotencyKey);
        }

        return b.build();
    }
}
