package de.thfamily18.restaurant_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import de.thfamily18.restaurant_backend.dto.payment.CapturePayPalOrderResponse;
import de.thfamily18.restaurant_backend.dto.payment.CreatePayPalOrderResponse;
import de.thfamily18.restaurant_backend.dto.payment.PayPalRefundResponse;
import de.thfamily18.restaurant_backend.entity.*;
import de.thfamily18.restaurant_backend.exception.ResourceNotFoundException;
import de.thfamily18.restaurant_backend.repository.OrderRepository;
import de.thfamily18.restaurant_backend.repository.RefundRepository;
import de.thfamily18.restaurant_backend.service.payment.PayPalAuthClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayPalPaymentService {

    private final RestClient.Builder restClientBuilder;
    private final PayPalAuthClient authClient;
    private final OrderRepository orderRepo;
    private final RefundRepository refundRepo;

    @Value("${paypal.base-url}")
    private String baseUrl;

    @Transactional
    public CreatePayPalOrderResponse createOrder(UUID orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can create PayPal payment");
        }

        if (order.getPaypalOrderId() != null && !order.getPaypalOrderId().isBlank()) {
            return new CreatePayPalOrderResponse(order.getPaypalOrderId());
        }

        JsonNode res = restClientBuilder.baseUrl(baseUrl).build()
                .post()
                .uri("/v2/checkout/orders")
                .headers(h -> {
                    h.setBearerAuth(authClient.getAccessToken());
                    h.set("PayPal-Request-Id", "create-order-" + order.getId());
                })
                .body(Map.of(
                        "intent", "CAPTURE",
                        "purchase_units", List.of(Map.of(
                                "reference_id", order.getId().toString(),
                                "description", "Order " + order.getId(),
                                "amount", Map.of(
                                        "currency_code", order.getCurrency(),
                                        "value", order.getTotalPrice().toPlainString()
                                )
                        ))
                ))
                .retrieve()
                .body(JsonNode.class);

        String paypalOrderId = res != null ? res.path("id").asText(null) : null;

        if (paypalOrderId == null || paypalOrderId.isBlank()) {
            throw new IllegalStateException("PayPal create order returned no id");
        }

        order.setPaypalOrderId(paypalOrderId);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);

        log.info("PayPal order created. orderId={}, paypalOrderId={}", order.getId(), paypalOrderId);

        return new CreatePayPalOrderResponse(paypalOrderId);
    }

    @Transactional
    public CapturePayPalOrderResponse captureOrder(String paypalOrderId) {
        Order order = orderRepo.findByPaypalOrderId(paypalOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found by PayPal order id: " + paypalOrderId
                ));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return new CapturePayPalOrderResponse(
                    order.getId(),
                    order.getPaypalOrderId(),
                    order.getPaypalCaptureId(),
                    order.getPaymentStatus()
            );
        }

        if (order.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new IllegalStateException("Order already refunded");
        }

        JsonNode res = restClientBuilder.baseUrl(baseUrl).build()
                .post()
                .uri("/v2/checkout/orders/{paypalOrderId}/capture", paypalOrderId)
                .headers(h -> {
                    h.setBearerAuth(authClient.getAccessToken());
                    h.set("PayPal-Request-Id", "capture-" + paypalOrderId);
                })
                .body(Map.of())
                .retrieve()
                .body(JsonNode.class);

        if (res == null) {
            throw new IllegalStateException("PayPal capture returned empty response");
        }

        String status = res.path("status").asText(null);
        String captureId = extractCaptureId(res);

        log.info("PayPal capture result. paypalOrderId={}, status={}, captureId={}",
                paypalOrderId, status, captureId);

        if (captureId != null && !captureId.isBlank()) {
            order.setPaypalCaptureId(captureId);
        }

        if ("COMPLETED".equalsIgnoreCase(status)) {
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaidAt(LocalDateTime.now());

        } else if ("PENDING".equalsIgnoreCase(status)) {
            order.setPaymentStatus(PaymentStatus.PROCESSING);

        } else {
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepo.save(order);

            throw new IllegalStateException("PayPal capture failed with status: " + status);
        }

        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);

        return new CapturePayPalOrderResponse(
                order.getId(),
                order.getPaypalOrderId(),
                order.getPaypalCaptureId(),
                order.getPaymentStatus()
        );
    }

    @Transactional
    public PayPalRefundResponse fullRefund(String paypalOrderId) {
        Order order = loadRefundableOrder(paypalOrderId);

        if (order.getPaypalCaptureId() == null || order.getPaypalCaptureId().isBlank()) {
            throw new IllegalStateException("Order has no PayPal capture id");
        }

        BigDecimal alreadyRefunded = order.getRefundedAmount() == null
                ? BigDecimal.ZERO
                : order.getRefundedAmount();

        BigDecimal remaining = order.getTotalPrice().subtract(alreadyRefunded);

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Order is already fully refunded");
        }

        return refund(order, remaining);
    }

    @Transactional
    public PayPalRefundResponse partialRefund(String paypalOrderId, BigDecimal amount) {
        Order order = loadRefundableOrder(paypalOrderId);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be greater than 0");
        }

        BigDecimal alreadyRefunded = order.getRefundedAmount() == null
                ? BigDecimal.ZERO
                : order.getRefundedAmount();

        if (alreadyRefunded.add(amount).compareTo(order.getTotalPrice()) > 0) {
            throw new IllegalArgumentException("Refund exceeds order total");
        }

        return refund(order, amount);
    }

    private PayPalRefundResponse refund(Order order, BigDecimal amount) {
        Map<String, Object> body = Map.of(
                "amount", Map.of(
                        "currency_code", order.getCurrency(),
                        "value", amount.toPlainString()
                )
        );

        // Stable idempotency key for the same order + same refund amount.
        // This prevents double refund when the same request is retried because of network timeout.
        String amountKey = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
        String idempotencyKey = "refund-" + order.getId() + "-" + amountKey;

        JsonNode res = restClientBuilder.baseUrl(baseUrl).build()
                .post()
                .uri("/v2/payments/captures/{captureId}/refund", order.getPaypalCaptureId())
                .headers(h -> {
                    h.setBearerAuth(authClient.getAccessToken());
                    h.set("PayPal-Request-Id", idempotencyKey);
                })
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (res == null) {
            throw new IllegalStateException("PayPal refund returned empty response");
        }

        String paypalRefundId = res.path("id").asText(null);
        String statusRaw = res.path("status").asText(null);

        if (paypalRefundId == null || paypalRefundId.isBlank()) {
            throw new IllegalStateException("PayPal refund returned no id");
        }

        RefundProviderStatus status = RefundProviderStatus.fromProvider(statusRaw);

        Refund refund = refundRepo.findByPaypalRefundId(paypalRefundId).orElse(null);

        if (refund == null) {
            refund = Refund.builder()
                    .order(order)
                    .paypalRefundId(paypalRefundId)
                    .paypalCaptureId(order.getPaypalCaptureId())
                    .amount(amount)
                    .status(status)
                    .createdAt(LocalDateTime.now())
                    .build();
        } else {
            refund.setStatus(status);
        }

        refundRepo.save(refund);

        BigDecimal totalRefunded = refundRepo.sumSucceededAmountByOrderId(order.getId());
        if (totalRefunded == null) {
            totalRefunded = BigDecimal.ZERO;
        }

        updateOrderRefundState(order, totalRefunded);

        log.info("PayPal refund requested. orderId={}, paypalRefundId={}, status={}, amount={}, totalRefunded={}",
                order.getId(), paypalRefundId, statusRaw, amount, totalRefunded);

        return new PayPalRefundResponse(
                order.getId(),
                paypalRefundId,
                order.getPaypalCaptureId(),
                totalRefunded,
                statusRaw
        );
    }

    private Order loadRefundableOrder(String paypalOrderId) {
        Order order = orderRepo.findByPaypalOrderId(paypalOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found by PayPal order id: " + paypalOrderId
                ));

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("Only PAID orders can be refunded");
        }

        if (order.getRefundStatus() == RefundStatus.REFUNDED
                || order.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new IllegalStateException("Order is already fully refunded");
        }

        return order;
    }

    private void updateOrderRefundState(Order order, BigDecimal totalRefunded) {
        order.setRefundedAmount(totalRefunded);

        BigDecimal totalPrice = order.getTotalPrice() == null
                ? BigDecimal.ZERO
                : order.getTotalPrice();

        if (totalRefunded.compareTo(BigDecimal.ZERO) == 0) {
            order.setRefundStatus(RefundStatus.REQUESTED);

        } else if (totalRefunded.compareTo(totalPrice) >= 0) {
            order.setRefundStatus(RefundStatus.REFUNDED);
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            order.setRefundedAt(LocalDateTime.now());

        } else {
            order.setRefundStatus(RefundStatus.PARTIAL);
        }

        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);
    }

    private String extractCaptureId(JsonNode res) {
        JsonNode capturesNode = res.path("purchase_units")
                .path(0)
                .path("payments")
                .path("captures");

        if (!capturesNode.isArray() || capturesNode.isEmpty()) {
            return null;
        }

        return capturesNode.path(0).path("id").asText(null);
    }
}