package de.thfamily18.restaurant_backend.controller;

import de.thfamily18.restaurant_backend.service.StripeWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final StripeWebhookService webhookService;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,                       // raw string
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        webhookService.handle(payload, sigHeader);
        return ResponseEntity.ok("ok");
    }
}

