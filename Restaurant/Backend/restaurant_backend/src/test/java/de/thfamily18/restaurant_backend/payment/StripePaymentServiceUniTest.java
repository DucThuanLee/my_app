package de.thfamily18.restaurant_backend.payment;

import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import de.thfamily18.restaurant_backend.dto.payment.CreateStripeIntentResponse;
import de.thfamily18.restaurant_backend.entity.Order;
import de.thfamily18.restaurant_backend.entity.OrderStatus;
import de.thfamily18.restaurant_backend.entity.PaymentStatus;
import de.thfamily18.restaurant_backend.repository.OrderRepository;
import de.thfamily18.restaurant_backend.service.StripePaymentService;
import de.thfamily18.restaurant_backend.service.payment.StripeGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
// Unit test bằng Mockito
class StripePaymentServiceUniTest {

    @Mock
    StripeGateway stripeGateway;
    @Mock
    OrderRepository orderRepo;

    @InjectMocks
    StripePaymentService service;

    @Test
    void createPaymentIntent_whenExistingPi_shouldRetrieveAndReturn() throws Exception {
        UUID orderId = UUID.randomUUID();

        Order order = new Order();
        order.setId(orderId);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setOrderStatus(OrderStatus.NEW);
        order.setCreatedAt(LocalDateTime.now());
        order.setTotalPrice(new BigDecimal("7.80"));
        order.setStripePaymentIntentId("pi_existing");

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        PaymentIntent existing = new PaymentIntent();
        existing.setId("pi_existing");
        existing.setClientSecret("cs_existing");
        when(stripeGateway.retrievePaymentIntent("pi_existing")).thenReturn(existing);

        ReflectionTestUtils.setField(service, "ttlMinutes", 30L);

        CreateStripeIntentResponse res = service.createPaymentIntent(orderId);

        assertEquals("pi_existing", res.paymentIntentId());
        assertEquals("cs_existing", res.clientSecret());

        verify(stripeGateway, never()).createPaymentIntent(any(PaymentIntentCreateParams.class), any());
    }

    @Test
    void createPaymentIntent_whenNew_shouldCreateAndPersistPiId() throws Exception {
        UUID orderId = UUID.randomUUID();

        Order order = new Order();
        order.setId(orderId);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setOrderStatus(OrderStatus.NEW);
        order.setCreatedAt(LocalDateTime.now());
        order.setTotalPrice(new BigDecimal("7.80"));

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        PaymentIntent created = new PaymentIntent();
        created.setId("pi_new");
        created.setClientSecret("cs_new");

        when(stripeGateway.createPaymentIntent(any(PaymentIntentCreateParams.class), any()))
                .thenReturn(created);

        // set secretKey để requestOptions có apiKey (optional)
        ReflectionTestUtils.setField(service, "stripeSecretKey", "sk_test_dummy");
        ReflectionTestUtils.setField(service, "ttlMinutes", 30L);

        CreateStripeIntentResponse res = service.createPaymentIntent(orderId);

        assertEquals("pi_new", res.paymentIntentId());
        assertEquals("cs_new", res.clientSecret());
        assertEquals("pi_new", order.getStripePaymentIntentId());

        verify(stripeGateway).createPaymentIntent(any(PaymentIntentCreateParams.class), any());
        verify(orderRepo).findById(orderId);
    }

    @Test
    void createPaymentIntent_whenOrderExpired_shouldThrow() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now().minusMinutes(31));
        order.setTotalPrice(new BigDecimal("7.80"));

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        ReflectionTestUtils.setField(service, "ttlMinutes", 30L);

        assertThrows(IllegalStateException.class, () -> service.createPaymentIntent(orderId));
        verifyNoInteractions(stripeGateway);
    }
}
