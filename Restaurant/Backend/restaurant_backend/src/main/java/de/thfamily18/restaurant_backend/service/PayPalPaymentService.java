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
import java.time.LocalDateTime;
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
                .headers(h -> h.setBearerAuth(authClient.getAccessToken()))
                .body(Map.of(
                        "intent", "CAPTURE",
                        "purchase_units", java.util.List.of(Map.of(
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

        String paypalOrderId = res.path("id").asText(null);
        if (paypalOrderId == null || paypalOrderId.isBlank()) {
            throw new IllegalStateException("PayPal create order returned no id");
        }

        order.setPaypalOrderId(paypalOrderId);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);

        return new CreatePayPalOrderResponse(paypalOrderId);
    }

    @Transactional
    public CapturePayPalOrderResponse captureOrder(String paypalOrderId) {
        Order order = orderRepo.findByPaypalOrderId(paypalOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found by PayPal order id: " + paypalOrderId));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return new CapturePayPalOrderResponse(order.getId(), order.getPaypalOrderId(), order.getPaypalCaptureId(), order.getPaymentStatus());
        }

        if (order.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new IllegalStateException("Order already refunded");
        }

        JsonNode res = restClientBuilder.baseUrl(baseUrl).build()
                .post()
                .uri("/v2/checkout/orders/{paypalOrderId}/capture", paypalOrderId)
                .headers(h -> h.setBearerAuth(authClient.getAccessToken()))
                .body(Map.of())
                .retrieve()
                .body(JsonNode.class);

        String status = res.path("status").asText(null);
        String captureId = res.path("purchase_units")
                .path(0).path("payments").path("captures")
                .path(0).path("id").asText(null);

        if ("COMPLETED".equalsIgnoreCase(status)) {
            order.setPaypalCaptureId(captureId);
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            orderRepo.save(order);
        }

        return new CapturePayPalOrderResponse(order.getId(), order.getPaypalOrderId(), order.getPaypalCaptureId(), order.getPaymentStatus());
    }

    @Transactional
    public PayPalRefundResponse fullRefund(String paypalOrderId) {
        Order order = loadPaidOrder(paypalOrderId);

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

        return refund(order, null);
    }

    @Transactional
    public PayPalRefundResponse partialRefund(String paypalOrderId, BigDecimal amount) {
        Order order = loadPaidOrder(paypalOrderId);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be greater than 0");
        }

        BigDecimal alreadyRefunded = order.getRefundedAmount() == null ? BigDecimal.ZERO : order.getRefundedAmount();

        if (alreadyRefunded.add(amount).compareTo(order.getTotalPrice()) > 0) {
            throw new IllegalArgumentException("Refund exceeds order total");
        }

        return refund(order, amount);
    }

    private PayPalRefundResponse refund(Order order, BigDecimal amount) {
        Map<String, Object> body = amount == null
                ? Map.of()
                : Map.of("amount", Map.of(
                "currency_code", order.getCurrency(),
                "value", amount.toPlainString()
        ));

        JsonNode res = restClientBuilder.baseUrl(baseUrl).build()
                .post()
                .uri("/v2/payments/captures/{captureId}/refund", order.getPaypalCaptureId())
                .headers(h -> h.setBearerAuth(authClient.getAccessToken()))
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        String paypalRefundId = res.path("id").asText(null);
        String statusRaw = res.path("status").asText(null);

        if (paypalRefundId == null || paypalRefundId.isBlank()) {
            throw new IllegalStateException("PayPal refund returned no id");
        }

        BigDecimal refundAmount = amount != null
                ? amount
                : order.getTotalPrice().subtract(order.getRefundedAmount() == null ? BigDecimal.ZERO : order.getRefundedAmount());

        RefundProviderStatus status = RefundProviderStatus.fromProvider(statusRaw);

        Refund refund = refundRepo.findByPaypalRefundId(paypalRefundId).orElse(null);

        if (refund == null) {
            refund = Refund.builder()
                    .order(order)
                    .paypalRefundId(paypalRefundId)
                    .paypalCaptureId(order.getPaypalCaptureId())
                    .amount(refundAmount)
                    .status(status)
                    .createdAt(LocalDateTime.now())
                    .build();
        } else {
            refund.setStatus(status);
        }

        refundRepo.save(refund);

        BigDecimal totalRefunded = refundRepo.sumSucceededAmountByOrderId(order.getId());
        if (totalRefunded == null) totalRefunded = BigDecimal.ZERO;

        updateOrderRefundState(order, totalRefunded);

        return new PayPalRefundResponse(
                order.getId(),
                paypalRefundId,
                order.getPaypalCaptureId(),
                totalRefunded,
                statusRaw
        );
    }

    private Order loadPaidOrder(String paypalOrderId) {
        Order order = orderRepo.findByPaypalOrderId(paypalOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found by PayPal order id: " + paypalOrderId));

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("Only PAID orders can be refunded");
        }

        return order;
    }

    private void updateOrderRefundState(Order order, BigDecimal totalRefunded) {
        order.setRefundedAmount(totalRefunded);

        if (totalRefunded.compareTo(BigDecimal.ZERO) == 0) {
            order.setRefundStatus(RefundStatus.REQUESTED);

        } else if (totalRefunded.compareTo(order.getTotalPrice()) >= 0) {
            order.setRefundStatus(RefundStatus.REFUNDED);
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            order.setRefundedAt(LocalDateTime.now());

        } else {
            order.setRefundStatus(RefundStatus.PARTIAL);
        }

        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);
    }
}