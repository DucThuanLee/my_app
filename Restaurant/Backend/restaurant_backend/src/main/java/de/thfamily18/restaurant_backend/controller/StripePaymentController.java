package de.thfamily18.restaurant_backend.controller;

import com.stripe.exception.StripeException;
import de.thfamily18.restaurant_backend.dto.payment.CreateStripeIntentRequest;
import de.thfamily18.restaurant_backend.dto.payment.CreateStripeIntentResponse;
import de.thfamily18.restaurant_backend.dto.payment.PaymentStatusResponse;
import de.thfamily18.restaurant_backend.service.StripePaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments/stripe")
@RequiredArgsConstructor
public class StripePaymentController {

    private final StripePaymentService stripePaymentService;

    @PostMapping(value = "/intents", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CreateStripeIntentResponse createIntent(@RequestBody @Valid CreateStripeIntentRequest req) throws StripeException {
        return stripePaymentService.createPaymentIntent(req.orderId());
    }

    @GetMapping("/status/{orderId}")
    public PaymentStatusResponse status(@PathVariable UUID orderId) {
        return stripePaymentService.getStatus(orderId);
    }
}
