package de.thfamily18.restaurant_backend.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;
import de.thfamily18.restaurant_backend.dto.payment.RefundResponse;
import de.thfamily18.restaurant_backend.entity.Order;
import de.thfamily18.restaurant_backend.entity.PaymentStatus;
import de.thfamily18.restaurant_backend.exception.ResourceNotFoundException;
import de.thfamily18.restaurant_backend.repository.OrderRepository;
import de.thfamily18.restaurant_backend.service.payment.StripeGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StripeRefundService {

    private final OrderRepository orderRepo;
    private final StripeGateway stripeGateway;

    @Value("${stripe.secretKey:}")
    private String stripeSecretKey;

    @Transactional
    public RefundResponse refundOrder(UUID orderId, BigDecimal amountEurOrNull, String reason) throws StripeException {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // 1) Only refund paid orders (webhook-source-of-truth style)
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            // If already REFUNDED -> idempotent response
            if (order.getPaymentStatus() == PaymentStatus.REFUNDED) {
                // idempotent: already refunded
                return new RefundResponse(
                        order.getId(),
                        order.getStripePaymentIntentId(),
                        order.getStripeRefundId(),
                        "already_refunded succeeded",                 // best guess; real status should come from Stripe
                        order.getPaymentStatus().name()
                );
            }

                // If refund already requested and you store stripeRefundId -> return that instead of failing (optional)
                if (order.getStripeRefundId() != null && !order.getStripeRefundId().isBlank()) {
                    Refund existing = safeRetrieveRefund(order.getStripeRefundId());
                    return new RefundResponse(
                            order.getId(),
                            order.getStripePaymentIntentId(),
                            order.getStripeRefundId(),
                            existing != null ? existing.getStatus() : "unknown",
                            order.getPaymentStatus().name()
                    );
            }
            throw new IllegalStateException("Order is not refundable in state: " + order.getPaymentStatus());
        }

        // 2) Must have a PaymentIntent id
        if (order.getStripePaymentIntentId() == null || order.getStripePaymentIntentId().isBlank()) {
            throw new IllegalStateException("Missing stripePaymentIntentId on order");
        }

        // 3) If already has refund id (idempotent) return
        if (order.getStripeRefundId() != null && !order.getStripeRefundId().isBlank()) {
            Refund existing = safeRetrieveRefund(order.getStripeRefundId());
            return new RefundResponse(
                    order.getId(),
                    order.getStripePaymentIntentId(),
                    order.getStripeRefundId(),
                    existing != null ? existing.getStatus() : "already_created",
                    order.getPaymentStatus().name()
            );

        }

        // 4) Amount (optional)
        Long amountCents = null;
        if (amountEurOrNull != null) {
            if (amountEurOrNull.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Refund amount must be > 0");
            }
            if (order.getTotalPrice() != null && amountEurOrNull.compareTo(order.getTotalPrice()) > 0) {
                throw new IllegalArgumentException("Refund amount must be <= order total");
            }
            amountCents = toCents(amountEurOrNull);
        }
        RefundCreateParams.Builder builder = RefundCreateParams.builder()
                .setPaymentIntent(order.getStripePaymentIntentId())
                .putMetadata("orderId", order.getId().toString());
        if (amountCents != null) {
            builder.setAmount(amountCents);
        }
        // Stripe reason is enum-like; keep as metadata unless you map it strictly
        if (reason != null && !reason.isBlank()) {
            builder.putMetadata("reason", reason);
        }

        RefundCreateParams params = builder.build();

        // 5) Stripe idempotency key: prevent duplicates on retries
        String idempotencyKey = "refund:order:" + order.getId()
                + (amountEurOrNull == null ? ":full" : ":partial:" + toCents(amountEurOrNull));

        Refund refund = stripeGateway.createRefund(params, requestOptions(idempotencyKey));

        // 6) Persist refund id (do NOT set REFUNDED yet if you want webhook as source of truth)
        // For MVP you can set immediately, but production: prefer webhook charge.refunded / refund.updated
        order.setStripeRefundId(refund.getId());
        order.setRefundRequestedAt(LocalDateTime.now());
        // Optional: store requested amount (if you added field)
//        if (amountEurOrNull != null) {
//            order.setRefundedAmount(amountEurOrNull);
//        }
        // Option A (strict): keep PAID until webhook confirms refund succeeded
        // Option B (simple): mark REFUNDED immediately
        // I chose Option A (production-safe):
        // order.setPaymentStatus(PaymentStatus.REFUNDED);
        // order.setRefundedAt(LocalDateTime.now());

        orderRepo.save(order);

        return new RefundResponse(
                order.getId(),
                order.getStripePaymentIntentId(),
                refund.getId(),
                refund.getStatus(),
                order.getPaymentStatus().name() // still PAID until webhook
        );
    }

    private Refund safeRetrieveRefund(String refundId) {
        try {
            return stripeGateway.retrieveRefund(refundId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private long toCents(BigDecimal eur) {
        return eur.movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private RequestOptions requestOptions(String idempotencyKey) {
        // Refund is an admin action; fail-fast if missing key
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new IllegalStateException("Missing STRIPE_SECRET_KEY");
        }
        RequestOptions.RequestOptionsBuilder b = RequestOptions.builder();
        b.setApiKey(stripeSecretKey);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            b.setIdempotencyKey(idempotencyKey);
        }
        return b.build();
    }
}
