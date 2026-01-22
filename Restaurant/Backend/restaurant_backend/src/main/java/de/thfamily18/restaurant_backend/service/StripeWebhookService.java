package de.thfamily18.restaurant_backend.service;

import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import de.thfamily18.restaurant_backend.entity.Order;
import de.thfamily18.restaurant_backend.entity.PaymentStatus;
import de.thfamily18.restaurant_backend.exception.ResourceNotFoundException;
import de.thfamily18.restaurant_backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StripeWebhookService {

    private final OrderRepository orderRepo;

    @Value("${stripe.webhookSecret}")
    private String webhookSecret;

    @Transactional
    public void handle(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (Exception e) {
            // Signature invalid -> 400 (you can throw an error in the controller)
            throw new IllegalArgumentException("Invalid Stripe signature", e);
        }

        String type = event.getType();

        if ("payment_intent.succeeded".equals(type)) {
            PaymentIntent pi = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject()
                    .orElse(null);

            if (pi == null) return;

            String orderId = pi.getMetadata().get("orderId");
            if (orderId == null) return;

            Order order = orderRepo.findById(UUID.fromString(orderId))
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

            // idempotent
            if (order.getPaymentStatus() == PaymentStatus.PAID) return;

            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
            order.setStripePaymentIntentId(pi.getId());
            return;
        }

        if ("payment_intent.payment_failed".equals(type)) {
            PaymentIntent pi = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject()
                    .orElse(null);

            if (pi == null) return;

            String orderId = pi.getMetadata().get("orderId");
            if (orderId == null) return;

            Order order = orderRepo.findById(UUID.fromString(orderId))
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

            if (order.getPaymentStatus() == PaymentStatus.PAID) return;

            order.setPaymentStatus(PaymentStatus.FAILED);
        }
    }
}

