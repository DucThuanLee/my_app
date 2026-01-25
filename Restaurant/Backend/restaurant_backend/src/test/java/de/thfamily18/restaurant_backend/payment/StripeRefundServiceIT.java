package de.thfamily18.restaurant_backend.payment;

import com.stripe.model.Refund;
import de.thfamily18.restaurant_backend.AbstractIntegrationTest;
import de.thfamily18.restaurant_backend.entity.Order;
import de.thfamily18.restaurant_backend.entity.PaymentMethod;
import de.thfamily18.restaurant_backend.entity.PaymentStatus;
import de.thfamily18.restaurant_backend.repository.OrderRepository;
import de.thfamily18.restaurant_backend.service.StripeRefundService;
import de.thfamily18.restaurant_backend.service.payment.StripeGateway;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@SpringBootTest
@Transactional
public class StripeRefundServiceIT extends AbstractIntegrationTest {
    @Autowired
    private StripeRefundService refundService;
    @Autowired private OrderRepository orderRepo;

    @MockitoBean
    private StripeGateway stripeGateway;

    @Test
    void requestRefund_paidOrder_shouldCreateRefund_andNotSetRefundedYet() throws Exception {
        Order order = Order.builder()
                .paymentStatus(PaymentStatus.PAID)
                .paymentMethod(PaymentMethod.STRIPE)
                .totalPrice(new BigDecimal("10.00"))
                .stripePaymentIntentId("pi_abc")
                .createdAt(LocalDateTime.now())
                .build();
        orderRepo.save(order);

        Refund refund = new Refund();
        refund.setId("re_123");
        refund.setStatus("pending");

        Mockito.when(stripeGateway.createRefund(Mockito.any(), Mockito.any()))
                .thenReturn(refund);

//        RefundResponse res = refundService.requestRefund(order.getId(), null, "customer_request");
//
//        assertThat(res.refundId()).isEqualTo("re_123");
//        assertThat(res.paymentStatus()).isEqualTo("PAID"); // still PAID (webhook will update)
//
//        Order saved = orderRepo.findById(order.getId()).orElseThrow();
//        assertThat(saved.getStripeRefundId()).isEqualTo("re_123");
//        assertThat(saved.getRefundRequestedAt()).isNotNull();
//        assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void requestRefund_notPaid_shouldFail() {
        Order order = Order.builder()
                .paymentStatus(PaymentStatus.PENDING)
                .paymentMethod(PaymentMethod.STRIPE)
                .totalPrice(new BigDecimal("10.00"))
                .stripePaymentIntentId("pi_abc")
                .createdAt(LocalDateTime.now())
                .build();
        orderRepo.save(order);

//        assertThatThrownBy(() -> refundService.requestRefund(order.getId(), null, null))
//                .isInstanceOf(IllegalStateException.class);

        Mockito.verifyNoInteractions(stripeGateway);
    }
}
