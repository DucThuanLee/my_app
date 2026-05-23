package de.thfamily18.restaurant_backend.controller;

import de.thfamily18.restaurant_backend.dto.payment.CapturePayPalOrderResponse;
import de.thfamily18.restaurant_backend.dto.payment.CreatePayPalOrderResponse;
import de.thfamily18.restaurant_backend.dto.payment.PayPalRefundResponse;
import de.thfamily18.restaurant_backend.service.PayPalPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments/paypal")
@RequiredArgsConstructor
public class PayPalController {

    private final PayPalPaymentService service;

    /**
     * =========================================================
     * CREATE PAYPAL ORDER
     * =========================================================
     */
    @PostMapping("/orders/{orderId}")
    @Operation(summary = "Create PayPal order")
    public ResponseEntity<CreatePayPalOrderResponse> create(
            @PathVariable UUID orderId
    ) {

        CreatePayPalOrderResponse response =
                service.createOrder(orderId);

        return ResponseEntity.ok(response);
    }

    /**
     * =========================================================
     * CAPTURE PAYMENT
     * =========================================================
     */
    @PostMapping("/orders/{paypalOrderId}/capture")
    @Operation(summary = "Capture PayPal payment")
    public ResponseEntity<CapturePayPalOrderResponse> capture(
            @PathVariable String paypalOrderId
    ) {

        CapturePayPalOrderResponse response =
                service.captureOrder(paypalOrderId);

        return ResponseEntity.ok(response);
    }

    /**
     * =========================================================
     * FULL REFUND
     * =========================================================
     */
    @PostMapping("/orders/{paypalOrderId}/refund")
    @Operation(summary = "Full refund PayPal payment")
    public ResponseEntity<PayPalRefundResponse> refundFull(
            @PathVariable String paypalOrderId
    ) {

        PayPalRefundResponse response =
                service.fullRefund(paypalOrderId);

        return ResponseEntity.ok(response);
    }

    /**
     * =========================================================
     * PARTIAL REFUND
     * =========================================================
     */
    @PostMapping("/orders/{paypalOrderId}/refund/partial")
    @Operation(summary = "Partial refund PayPal payment")
    public ResponseEntity<PayPalRefundResponse> refundPartial(
            @PathVariable String paypalOrderId,
            @RequestParam
            @DecimalMin(value = "0.01")
            BigDecimal amount
    ) {

        PayPalRefundResponse response =
                service.partialRefund(paypalOrderId, amount);

        return ResponseEntity.ok(response);
    }
}