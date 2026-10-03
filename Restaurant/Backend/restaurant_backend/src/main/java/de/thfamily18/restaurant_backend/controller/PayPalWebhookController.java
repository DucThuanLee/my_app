package de.thfamily18.restaurant_backend.controller;

import de.thfamily18.restaurant_backend.service.PayPalWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/paypal")
@RequiredArgsConstructor
@Slf4j
public class PayPalWebhookController {

    private final PayPalWebhookService webhookService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,

            @RequestHeader(name = "paypal-transmission-id")
            String transmissionId,

            @RequestHeader(name = "paypal-transmission-time")
            String transmissionTime,

            @RequestHeader(name = "paypal-transmission-sig")
            String transmissionSig,

            @RequestHeader(name = "paypal-cert-url")
            String certUrl,

            @RequestHeader(name = "paypal-auth-algo")
            String authAlgo
    ) {
        log.info("Received PayPal webhook. transmissionId={}", transmissionId);
        log.debug("RAW PayPal webhook payload={}", payload);

        try {
            webhookService.handle(
                    payload,
                    transmissionId,
                    transmissionTime,
                    transmissionSig,
                    certUrl,
                    authAlgo
            );

            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException e) {
            // Invalid payload/signature. Retrying will not fix this.
            log.warn("PayPal webhook rejected. transmissionId={}, reason={}",
                    transmissionId, e.getMessage());

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            // Internal error: DB down, PayPal verifier timeout, transaction failure, etc.
            // Return 500 so PayPal can retry later.
            log.error("Unexpected error while processing PayPal webhook. transmissionId={}",
                    transmissionId, e);

            return ResponseEntity.internalServerError().build();
        }
    }
}