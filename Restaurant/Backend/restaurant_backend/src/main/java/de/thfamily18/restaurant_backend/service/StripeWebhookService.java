package de.thfamily18.restaurant_backend.service;

import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import de.thfamily18.restaurant_backend.entity.Order;
import de.thfamily18.restaurant_backend.entity.PaymentStatus;
import de.thfamily18.restaurant_backend.exception.ResourceNotFoundException;
import de.thfamily18.restaurant_backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookService {

    private final OrderRepository orderRepo;

    @Value("${stripe.webhookSecret}")
    private String webhookSecret;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handle(String payload, String sigHeader) {
        final Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Stripe signature", e);
        }

        String type = event.getType();
        if (!type.equals("payment_intent.succeeded") && !type.equals("payment_intent.payment_failed")) {
            return;
        }

        // Parse raw payload (reliable)
        final JsonNode obj;
        try {
            obj = objectMapper.readTree(payload).path("data").path("object");
        } catch (Exception e) {
            log.warn("Cannot parse webhook payload. eventId={}", event.getId(), e);
            return;
        }

        String piId = obj.path("id").asText(null);
        String orderId = obj.path("metadata").path("orderId").asText(null);

        log.info("Stripe webhook type={}, eventId={}, piId={}, orderId={}",
                type, event.getId(), piId, orderId);

        if (orderId == null || orderId.isBlank()) {
            log.warn("Missing orderId in metadata. piId={}", piId);
            return;
        }

        Order order = orderRepo.findById(UUID.fromString(orderId))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        // idempotent
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            log.info("Order already PAID. orderId={}", orderId);
            return;
        }

        if (type.equals("payment_intent.succeeded")) {
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
            order.setStripePaymentIntentId(piId);
        } else {
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setStripePaymentIntentId(piId);
        }

        orderRepo.save(order);
        orderRepo.flush();

        log.info("Order updated. orderId={}, paymentStatus={}", orderId, order.getPaymentStatus());
    }

}

