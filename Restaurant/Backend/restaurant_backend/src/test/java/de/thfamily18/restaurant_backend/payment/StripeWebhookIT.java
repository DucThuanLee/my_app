package de.thfamily18.restaurant_backend.payment;

import de.thfamily18.restaurant_backend.AbstractIntegrationTest;
import de.thfamily18.restaurant_backend.entity.Order;
import de.thfamily18.restaurant_backend.entity.PaymentStatus;
import de.thfamily18.restaurant_backend.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class StripeWebhookIT extends AbstractIntegrationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    OrderRepository orderRepo;
    @Autowired
    ObjectMapper om;

    private static final String WEBHOOK_SECRET =
            "whsec_test_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @DynamicPropertySource
    static void stripeWebhookProps(DynamicPropertyRegistry r) {
        // Đảm bảo service verify signature dùng đúng secret này
        r.add("stripe.webhook.secret", () -> WEBHOOK_SECRET);
    }

    @Test
    void webhook_paymentIntentSucceeded_shouldMarkOrderPaid() throws Exception {
        // 1) Create order in DB (real Postgres)
        Order order = new Order();
        order.setId(UUID.randomUUID()); // nếu ID tự generate thì bỏ dòng này
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order = orderRepo.save(order);

        // 2) Build Stripe-like payload
        String payload = """
        {
          "id": "evt_test_1",
          "object": "event",
          "type": "payment_intent.succeeded",
          "data": {
            "object": {
              "id": "pi_test_123",
              "object": "payment_intent",
              "metadata": {
                "orderId": "%s"
              }
            }
          }
        }
        """.formatted(order.getId());

        // 3) Create a valid Stripe-Signature header for this payload
        String sigHeader = stripeSignatureHeader(payload, WEBHOOK_SECRET);

        // 4) Call webhook endpoint
        mvc.perform(post("/api/payments/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sigHeader)
                        .content(payload))
                .andExpect(status().isOk());

        // 5) Assert DB updated
        Order updated = orderRepo.findById(order.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, updated.getPaymentStatus());
        assertNotNull(updated.getPaidAt(), "paidAt should be set");
        assertEquals("pi_test_123", updated.getStripePaymentIntentId());
    }

    @Test
    void webhook_paymentIntentFailed_shouldMarkOrderFailed() throws Exception {
        Order order = new Order();
        order.setId(UUID.randomUUID()); // nếu ID auto thì bỏ
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order = orderRepo.save(order);

        String payload = """
        {
          "id": "evt_test_2",
          "object": "event",
          "type": "payment_intent.payment_failed",
          "data": {
            "object": {
              "id": "pi_test_999",
              "object": "payment_intent",
              "metadata": {
                "orderId": "%s"
              }
            }
          }
        }
        """.formatted(order.getId());

        String sigHeader = stripeSignatureHeader(payload, WEBHOOK_SECRET);

        mvc.perform(post("/api/payments/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sigHeader)
                        .content(payload))
                .andExpect(status().isOk());

        Order updated = orderRepo.findById(order.getId()).orElseThrow();
        assertEquals(PaymentStatus.FAILED, updated.getPaymentStatus());
        assertEquals("pi_test_999", updated.getStripePaymentIntentId());
    }

    @Test
    void webhook_invalidSignature_should400_orThrow() throws Exception {
        Order order = new Order();
        order.setId(UUID.randomUUID()); // nếu ID auto thì bỏ
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order = orderRepo.save(order);

        String payload = """
        {
          "id": "evt_test_3",
          "object": "event",
          "type": "payment_intent.succeeded",
          "data": {
            "object": {
              "id": "pi_test_bad",
              "object": "payment_intent",
              "metadata": { "orderId": "%s" }
            }
          }
        }
        """.formatted(order.getId());

        // signature cố tình sai
        String sigHeader = "t=1700000000,v1=deadbeef";

        // Controller của bạn hiện throw IllegalArgumentException trong service.
        // Tuỳ GlobalExceptionHandler của bạn map ra 400/500.
        mvc.perform(post("/api/payments/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sigHeader)
                        .content(payload))
                .andExpect(status().isBadRequest()); // nếu bạn map IllegalArgumentException => 400
    }

    /**
     * Build Stripe-Signature header:
     *   t=timestamp,v1=HMAC_SHA256_HEX( (timestamp + "." + payload), secret )
     */
    private static String stripeSignatureHeader(String payload, String webhookSecret) throws Exception {
        long ts = System.currentTimeMillis() / 1000;

        String signedPayload = ts + "." + payload;
        String v1 = hmacSha256Hex(webhookSecret, signedPayload);

        return "t=" + ts + ",v1=" + v1;
    }

    private static String hmacSha256Hex(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // Java 17+ (bạn đang Java 21): HexFormat tiện nhất
        return java.util.HexFormat.of().formatHex(raw);
    }
}
