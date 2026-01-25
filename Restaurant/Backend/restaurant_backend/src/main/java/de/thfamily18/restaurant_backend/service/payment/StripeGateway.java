package de.thfamily18.restaurant_backend.service.payment;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StripeGateway {

    private final StripeClient stripe;

    // ===== PaymentIntent =====

    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        return stripe.paymentIntents().retrieve(paymentIntentId);
    }

    public PaymentIntent createPaymentIntent(PaymentIntentCreateParams params, RequestOptions requestOptions) throws StripeException {
        if (requestOptions == null) {
            return stripe.paymentIntents().create(params);
        }
        return stripe.paymentIntents().create(params, requestOptions);
    }

    // Convenience overload (optional)
    public PaymentIntent createPaymentIntent(PaymentIntentCreateParams params) throws StripeException {
        return stripe.paymentIntents().create(params);
    }

    // ===== Refund =====

    public Refund createRefund(RefundCreateParams params, RequestOptions requestOptions) throws StripeException {
        if (requestOptions == null) {
            return stripe.refunds().create(params);
        }
        return stripe.refunds().create(params, requestOptions);
    }

    public Refund retrieveRefund(String refundId) throws StripeException {
        return stripe.refunds().retrieve(refundId);
    }
}
