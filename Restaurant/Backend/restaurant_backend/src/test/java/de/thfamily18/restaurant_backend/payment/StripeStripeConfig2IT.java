package de.thfamily18.restaurant_backend.payment;

import de.thfamily18.restaurant_backend.AbstractIntegrationTest;
import de.thfamily18.restaurant_backend.entity.Order;
import de.thfamily18.restaurant_backend.entity.PaymentStatus;
import de.thfamily18.restaurant_backend.repository.OrderRepository;
import de.thfamily18.restaurant_backend.service.StripePaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// using stripe/stripe-mock
@Testcontainers
public class StripeStripeConfig2IT extends AbstractIntegrationTest {
    @Container
    static final GenericContainer<?> stripeMock =
            new GenericContainer<>("stripe/stripe-mock:latest")
                    .withExposedPorts(12111); // HTTP port

    @DynamicPropertySource
    static void stripeProps(DynamicPropertyRegistry r) {
        r.add("stripe.api.base-url",
                () -> "http://" + stripeMock.getHost() + ":" + stripeMock.getMappedPort(12111));
        r.add("stripe.api.key", () -> "sk_test_dummy");
    }

    @Autowired
    StripePaymentService stripePaymentService;
    @Autowired
    OrderRepository orderRepo;

    @Test
    @Transactional
    void createPaymentIntent_shouldWorkAgainstStripeMock_andPersist() throws Exception {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setPaymentStatus(PaymentStatus.PENDING);
        order = orderRepo.save(order);

        var pi = stripePaymentService.createPaymentIntent(order.getId());

        assertNotNull(pi.paymentIntentId());
        assertNotNull(pi.clientSecret());

        // If your service includes this, set stripePaymentIntentId in the order:
        Order updated = orderRepo.findById(order.getId()).orElseThrow();
        assertNotNull(updated.getStripePaymentIntentId());
    }
}
