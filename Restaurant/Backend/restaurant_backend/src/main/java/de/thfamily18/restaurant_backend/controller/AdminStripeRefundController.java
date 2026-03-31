package de.thfamily18.restaurant_backend.controller;

import com.stripe.exception.StripeException;
import de.thfamily18.restaurant_backend.dto.payment.CreateRefundRequest;
import de.thfamily18.restaurant_backend.dto.payment.RefundResponse;
import de.thfamily18.restaurant_backend.service.StripeRefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/payments/stripe")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Payments")
public class AdminStripeRefundController {

    private final StripeRefundService refundService;

    @PostMapping("/refunds")
    @Operation(
            summary = "Create refund for an order",
            description = """
                    Create a refund via Stripe.

                    - Full refund: amount = null
                    - Partial refund: amount > 0

                    Notes:
                    - This API only REQUESTS refund.
                    - Final status is determined by Stripe webhook.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Refund created successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request (e.g. amount > order total)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Order not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Order already refunded"
            )
    })
    public ResponseEntity<RefundResponse> refund(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Refund request payload",
                    required = true
            )
            @Valid @RequestBody CreateRefundRequest req
    ) throws StripeException {

        RefundResponse res = refundService.refundOrder(
                req.orderId(),
                req.amount(),
                req.reason()
        );

        return ResponseEntity.ok(res);
    }
}
