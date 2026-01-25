package de.thfamily18.restaurant_backend.controller;

import com.stripe.exception.StripeException;
import de.thfamily18.restaurant_backend.dto.payment.CreateRefundRequest;
import de.thfamily18.restaurant_backend.dto.payment.RefundResponse;
import de.thfamily18.restaurant_backend.service.StripeRefundService;
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

    /**
     * Create refund for an order.
     *
     * - Full refund: amount = null
     * - Partial refund: amount > 0
     *
     * NOTE:
     * - PaymentStatus will NOT be set to REFUNDED here.
     * - Webhook (charge.refunded / refund.updated) is the source of truth.
     */
    @PostMapping("/refunds")
    public ResponseEntity<RefundResponse> refund(
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
