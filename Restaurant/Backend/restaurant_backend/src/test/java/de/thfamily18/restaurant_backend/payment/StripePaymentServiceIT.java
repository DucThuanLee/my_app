package de.thfamily18.restaurant_backend.payment;

import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.service.PaymentIntentService;
import de.thfamily18.restaurant_backend.AbstractIntegrationTest;
import de.thfamily18.restaurant_backend.dto.payment.CreateStripeIntentResponse;
import de.thfamily18.restaurant_backend.entity.Order;
import de.thfamily18.restaurant_backend.entity.OrderStatus;
import de.thfamily18.restaurant_backend.entity.PaymentMethod;
import de.thfamily18.restaurant_backend.entity.PaymentStatus;
import de.thfamily18.restaurant_backend.repository.OrderRepository;
import de.thfamily18.restaurant_backend.service.StripePaymentService;
import de.thfamily18.restaurant_backend.service.payment.StripeGateway;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Transactional
//@ActiveProfiles("test")
// using mockito to avoid stripe API calls
public class StripePaymentServiceIT extends AbstractIntegrationTest {
    @Autowired
    private StripePaymentService stripePaymentService;

    @Autowired
    private OrderRepository orderRepo;

    // Mock exactly within the boundary that Stripe Payment Service calls.
    @MockitoBean
    private StripeGateway stripeGateway;

    @Test
    void createPaymentIntent_ok_shouldCreateAndPersistPiId() throws Exception {
        // given
        Order order = Order.builder()
                .customerName("Test User")
                .paymentMethod(PaymentMethod.STRIPE)
                .paymentStatus(PaymentStatus.PENDING)
                .orderStatus(OrderStatus.NEW)
                .totalPrice(new BigDecimal("12.30"))
                .createdAt(LocalDateTime.now())
                .build();
        orderRepo.save(order);

        // mock Stripe


        PaymentIntent pi = new PaymentIntent();
        pi.setId("pi_test_123");
        pi.setClientSecret("secret_123");

        // when StripePaymentService calls gateway.createPaymentIntent(params, requestOptions)
        Mockito.when(stripeGateway.createPaymentIntent(Mockito.any(), Mockito.any()))
                .thenReturn(pi);
        // when
        CreateStripeIntentResponse res =
                stripePaymentService.createPaymentIntent(order.getId());

        // then
        assertThat(res.paymentIntentId()).isEqualTo("pi_test_123");
        assertThat(res.clientSecret()).isEqualTo("secret_123");

        Order saved = orderRepo.findById(order.getId()).orElseThrow();
        assertThat(saved.getStripePaymentIntentId()).isEqualTo("pi_test_123");
        assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        // verify create called once
        Mockito.verify(stripeGateway, Mockito.times(1))
                .createPaymentIntent(Mockito.any(), Mockito.any());
    }

    @Test
    void createPaymentIntent_twice_shouldReuseExistingIntent() throws Exception {
        // given
        Order order = Order.builder()
                .paymentStatus(PaymentStatus.PENDING)
                .totalPrice(new BigDecimal("5.00"))
                .createdAt(LocalDateTime.now())
                .stripePaymentIntentId("pi_existing")
                .build();
        orderRepo.save(order);

        PaymentIntent existing = new PaymentIntent();
        existing.setId("pi_existing");
        existing.setClientSecret("secret_existing");

        Mockito.when(stripeGateway.retrievePaymentIntent("pi_existing"))
                .thenReturn(existing);

        // when
        CreateStripeIntentResponse res =
                stripePaymentService.createPaymentIntent(order.getId());

        // then
        assertThat(res.paymentIntentId()).isEqualTo("pi_existing");
        assertThat(res.clientSecret()).isEqualTo("secret_existing");

        // verify NOT creating new PI
        Mockito.verify(stripeGateway, Mockito.never())
                .createPaymentIntent(Mockito.any(), Mockito.any());
        Mockito.verify(stripeGateway, Mockito.times(1))
                .retrievePaymentIntent("pi_existing");
    }

    @Test
    void createPaymentIntent_paidOrder_shouldFail() {
        Order order = Order.builder()
                .paymentStatus(PaymentStatus.PAID)
                .totalPrice(new BigDecimal("5.00"))
                .createdAt(LocalDateTime.now())
                .build();
        orderRepo.save(order);

        assertThatThrownBy(() -> stripePaymentService.createPaymentIntent(order.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already paid");

        Mockito.verifyNoInteractions(stripeGateway);
    }

    @Test
    void createPaymentIntent_expiredOrder_shouldFail() {
        Order order = Order.builder()
                .paymentMethod(PaymentMethod.STRIPE)
                .paymentStatus(PaymentStatus.PENDING)
                .totalPrice(new BigDecimal("5.00"))
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();
        orderRepo.save(order);

        assertThatThrownBy(() -> stripePaymentService.createPaymentIntent(order.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");

        Mockito.verifyNoInteractions(stripeGateway);
    }
    @Test
    void createPaymentIntent_shouldSendCorrectAmountInCents_rounded() throws Exception {
        // 7.805 EUR -> 781 cents (HALF_UP)
        Order order = Order.builder()
                .paymentMethod(PaymentMethod.STRIPE)
                .paymentStatus(PaymentStatus.PENDING)
                .totalPrice(new BigDecimal("7.805"))
                .createdAt(LocalDateTime.now())
                .build();
        orderRepo.save(order);

        PaymentIntent pi = new PaymentIntent();
        pi.setId("pi_test_amount");
        pi.setClientSecret("secret_amount");

        Mockito.when(stripeGateway.createPaymentIntent(Mockito.any(), Mockito.any()))
                .thenReturn(pi);

        stripePaymentService.createPaymentIntent(order.getId());

        // capture params to assert amount
        var paramsCaptor = org.mockito.ArgumentCaptor.forClass(PaymentIntentCreateParams.class);
        Mockito.verify(stripeGateway).createPaymentIntent(paramsCaptor.capture(), Mockito.any());

        PaymentIntentCreateParams sent = paramsCaptor.getValue();
        assertThat(sent.getAmount()).isEqualTo(781L);
        assertThat(sent.getCurrency()).isEqualTo("eur");
    }
}
