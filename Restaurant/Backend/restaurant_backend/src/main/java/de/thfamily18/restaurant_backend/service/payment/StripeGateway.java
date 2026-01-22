package de.thfamily18.restaurant_backend.service.payment;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StripeGateway {

    private final StripeClient stripe;

    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        return stripe.paymentIntents().retrieve(paymentIntentId);
    }

    public PaymentIntent createPaymentIntent(PaymentIntentCreateParams params) throws StripeException {
        return stripe.paymentIntents().create(params);
    }
}
