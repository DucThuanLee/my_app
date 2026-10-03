package de.thfamily18.restaurant_backend.service;

import de.thfamily18.restaurant_backend.entity.*;
import de.thfamily18.restaurant_backend.notification.NotificationService;
import de.thfamily18.restaurant_backend.repository.OrderRepository;
import de.thfamily18.restaurant_backend.repository.RefundRepository;
import de.thfamily18.restaurant_backend.service.payment.PayPalWebhookVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayPalWebhookService {

    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            "PAYMENT.CAPTURE.COMPLETED",
            "PAYMENT.CAPTURE.PENDING",
            "PAYMENT.CAPTURE.DENIED",
            "PAYMENT.CAPTURE.DECLINED",
            "PAYMENT.CAPTURE.REFUNDED"
    );

    private final ObjectMapper objectMapper;
    private final PayPalWebhookVerifier verifier;
    private final OrderRepository orderRepo;
    private final RefundRepository refundRepo;
    private final NotificationService notificationService;

    @Transactional
    public void handle(
            String payload,
            String transmissionId,
            String transmissionTime,
            String transmissionSig,
            String certUrl,
            String authAlgo
    ) {
        final JsonNode root;
        try {
            root = objectMapper.readTree(payload);
            // ===== DEBUG FULL PAYLOAD =====
            log.info("RAW PayPal webhook payload = {}", root.toPrettyString());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid PayPal webhook payload", e);
        }

        String eventType = root.path("event_type").asText(null);

        if (eventType == null || !SUPPORTED_EVENTS.contains(eventType)) {
            log.debug("Ignored PayPal webhook type={}", eventType);
            return;
        }

        boolean verified = verifier.verify(
                transmissionId,
                transmissionTime,
                transmissionSig,
                certUrl,
                authAlgo,
                root
        );

        if (!verified) {
            throw new IllegalArgumentException("Invalid PayPal webhook signature");
        }

        JsonNode resource = root.path("resource");
        String paypalOrderId = extractPaypalOrderId(root, resource);

        log.info("PayPal webhook verified. type={}, paypalOrderId={}", eventType, paypalOrderId);

        switch (eventType) {
            case "PAYMENT.CAPTURE.COMPLETED" -> handleCaptureCompleted(resource, paypalOrderId);
            case "PAYMENT.CAPTURE.PENDING" -> handleCapturePending(resource, paypalOrderId);
            case "PAYMENT.CAPTURE.DENIED", "PAYMENT.CAPTURE.DECLINED" -> handleCaptureFailed(resource, paypalOrderId);
            case "PAYMENT.CAPTURE.REFUNDED" -> handleCaptureRefunded(resource);
            default -> log.debug("Unhandled PayPal webhook type={}", eventType);
        }
    }

    private void handleCaptureCompleted(JsonNode resource, String paypalOrderId) {
        String captureId = resource.path("id").asText(null);

        if (paypalOrderId == null || paypalOrderId.isBlank()) {
            log.warn("Missing paypalOrderId for capture completed. captureId={}", captureId);
            return;
        }

        Order order = orderRepo.findByPaypalOrderId(paypalOrderId).orElse(null);
        if (order == null) {
            log.warn("Order not found. paypalOrderId={}, captureId={}", paypalOrderId, captureId);
            return;
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID
                || order.getPaymentStatus() == PaymentStatus.REFUNDED) {
            log.info("PayPal capture completed ignored. orderId={}, status={}",
                    order.getId(), order.getPaymentStatus());
            return;
        }

        order.setPaypalCaptureId(captureId);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        orderRepo.save(order);

        registerAfterCommit(() -> enqueuePaymentSucceeded(order));

        log.info("PayPal payment marked PAID. orderId={}, paypalOrderId={}, captureId={}",
                order.getId(), paypalOrderId, captureId);
    }

    private void handleCapturePending(JsonNode resource, String paypalOrderId) {
        String captureId = resource.path("id").asText(null);

        if (paypalOrderId == null || paypalOrderId.isBlank()) {
            log.warn("Missing paypalOrderId for capture pending. captureId={}", captureId);
            return;
        }

        Order order = orderRepo.findByPaypalOrderId(paypalOrderId).orElse(null);
        if (order == null) {
            log.warn("Order not found for pending capture. paypalOrderId={}, captureId={}", paypalOrderId, captureId);
            return;
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID
                || order.getPaymentStatus() == PaymentStatus.REFUNDED) {
            return;
        }

        order.setPaypalCaptureId(captureId);
        order.setPaymentStatus(PaymentStatus.PROCESSING);
        order.setUpdatedAt(LocalDateTime.now());

        orderRepo.save(order);

        log.info("PayPal payment marked PROCESSING. orderId={}, paypalOrderId={}, captureId={}",
                order.getId(), paypalOrderId, captureId);
    }

    private void handleCaptureFailed(JsonNode resource, String paypalOrderId) {
        String captureId = resource.path("id").asText(null);

        if (paypalOrderId == null || paypalOrderId.isBlank()) {
            log.warn("Missing paypalOrderId for capture failed. captureId={}", captureId);
            return;
        }

        Order order = orderRepo.findByPaypalOrderId(paypalOrderId).orElse(null);
        if (order == null) {
            log.warn("Order not found for failed capture. paypalOrderId={}, captureId={}", paypalOrderId, captureId);
            return;
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID
                || order.getPaymentStatus() == PaymentStatus.REFUNDED) {
            return;
        }

        order.setPaypalCaptureId(captureId);
        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setUpdatedAt(LocalDateTime.now());

        orderRepo.save(order);

        log.info("PayPal payment marked FAILED. orderId={}, paypalOrderId={}, captureId={}",
                order.getId(), paypalOrderId, captureId);
    }

    private void handleCaptureRefunded(JsonNode resource) {
        String paypalRefundId = resource.path("id").asText(null);

        String paypalCaptureId = resource.path("capture_id").asText(null);
        if (paypalCaptureId == null || paypalCaptureId.isBlank()) {
            paypalCaptureId = resource.path("supplementary_data")
                    .path("related_ids")
                    .path("capture_id")
                    .asText(null);
        }

        if (paypalRefundId == null || paypalRefundId.isBlank()) {
            log.warn("Missing paypalRefundId");
            return;
        }

        if (paypalCaptureId == null || paypalCaptureId.isBlank()) {
            log.warn("Missing paypalCaptureId. refundId={}", paypalRefundId);
            return;
        }

        Order order = orderRepo.findByPaypalCaptureId(paypalCaptureId).orElse(null);
        if (order == null) {
            log.warn("Order not found by PayPal captureId. paypalCaptureId={}, refundId={}",
                    paypalCaptureId, paypalRefundId);
            return;
        }

        BigDecimal amount = readAmount(resource.path("amount"));
        RefundProviderStatus status = RefundProviderStatus.fromProvider(
                resource.path("status").asText("COMPLETED")
        );

        Refund refund = refundRepo.findByPaypalRefundId(paypalRefundId).orElse(null);

        if (refund == null) {
            refund = Refund.builder()
                    .order(order)
                    .paypalRefundId(paypalRefundId)
                    .paypalCaptureId(paypalCaptureId)
                    .amount(amount)
                    .status(status)
                    .createdAt(LocalDateTime.now())
                    .build();
        } else {
            refund.setStatus(status);
            refund.setUpdatedAt(LocalDateTime.now());
        }

        refundRepo.save(refund);

        BigDecimal totalRefunded = Optional
                .ofNullable(refundRepo.sumSucceededAmountByOrderId(order.getId()))
                .orElse(BigDecimal.ZERO);

        updateOrderRefundState(order, totalRefunded, paypalCaptureId);

        log.info("PayPal refund synced. orderId={}, paypalRefundId={}, amount={}, status={}, totalRefunded={}",
                order.getId(), paypalRefundId, amount, status, totalRefunded);
    }

    private void updateOrderRefundState(Order order, BigDecimal totalRefunded, String paypalCaptureId) {
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

                registerAfterCommit(() -> enqueueRefundSucceeded(order, paypalCaptureId));
            }

        } else {
            order.setRefundStatus(RefundStatus.PARTIAL);
        }

        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);
    }

    private String extractPaypalOrderId(JsonNode root, JsonNode resource) {
        String orderId = root.path("supplementary_data")
                .path("related_ids")
                .path("order_id")
                .asText(null);

        if (orderId != null && !orderId.isBlank()) {
            return orderId;
        }

        orderId = resource.path("supplementary_data")
                .path("related_ids")
                .path("order_id")
                .asText(null);

        if (orderId != null && !orderId.isBlank()) {
            return orderId;
        }

        JsonNode links = resource.path("links");
        if (links.isArray()) {
            for (JsonNode link : links) {
                String rel = link.path("rel").asText(null);
                String href = link.path("href").asText("");

                if ("up".equalsIgnoreCase(rel)
                        && href.contains("/v2/checkout/orders/")) {
                    return href.substring(href.lastIndexOf("/") + 1);
                }
            }
        }

        return null;
    }

    private BigDecimal readAmount(JsonNode amountNode) {
        String valStr = amountNode.path("value").asText("0");

        if (valStr == null || valStr.isBlank()) {
            return BigDecimal.ZERO;
        }

        return new BigDecimal(valStr);
    }

    private void enqueuePaymentSucceeded(Order order) {
        String email = resolveEmail(order);
        if (email == null || email.isBlank()) return;

        Map<String, Object> vars = Map.of(
                "orderId", order.getId().toString(),
                "totalPrice", order.getTotalPrice() == null ? null : order.getTotalPrice().toPlainString(),
                "paidAt", order.getPaidAt() == null ? null : order.getPaidAt().toString()
        );

        notificationService.enqueuePaymentSucceeded(order.getId(), email, vars);
    }

    private void enqueueRefundSucceeded(Order order, String paypalCaptureId) {
        String email = resolveEmail(order);
        if (email == null || email.isBlank()) return;

        Map<String, Object> vars = Map.of(
                "orderId", order.getId().toString(),
                "refundedAt", order.getRefundedAt() == null ? null : order.getRefundedAt().toString(),
                "refundedAmount", order.getRefundedAmount() == null ? null : order.getRefundedAmount().toPlainString(),
                "chargeId", paypalCaptureId
        );

        notificationService.enqueueRefundSucceeded(order.getId(), email, vars);
    }

    private String resolveEmail(Order order) {
        if (order.getUser() != null && order.getUser().getEmail() != null) {
            return order.getUser().getEmail();
        }
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