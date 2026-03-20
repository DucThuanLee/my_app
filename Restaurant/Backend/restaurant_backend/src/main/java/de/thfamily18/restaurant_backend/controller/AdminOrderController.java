package de.thfamily18.restaurant_backend.controller;

// Admin manage orders

import de.thfamily18.restaurant_backend.dto.OrderResponse;
import de.thfamily18.restaurant_backend.dto.UpdateOrderStatusRequest;
import de.thfamily18.restaurant_backend.entity.OrderStatus;
import de.thfamily18.restaurant_backend.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
// GET /api/admin/orders?page=0&size=20
// GET /api/admin/orders?page=0&size=20&sortBy=createdAt&sortDir=asc
// GET /api/admin/orders?status=NEW&page=0&size=10&sortBy=totalPrice&sortDir=desc
public class AdminOrderController {

    private final OrderService service;

    @GetMapping
    @Operation(
            summary = "List orders for admin",
            description = "Returns paginated orders with optional status filter and sorting."
    )
    public Page<OrderResponse> list(
            @Parameter(description = "Filter by order status", example = "NEW")
            @RequestParam(required = false) OrderStatus status,

            @Parameter(description = "Page index (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(
                    description = "Sort field",
                    schema = @Schema(allowableValues = {
                            "createdAt", "totalPrice", "orderStatus", "paymentStatus"
                    }),
                    example = "createdAt"
            )
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(
                    description = "Sort direction",
                    schema = @Schema(allowableValues = {"asc", "desc"}),
                    example = "desc"
            )
            @RequestParam(defaultValue = "desc") String sortDir,

            @RequestHeader(name = "Accept-Language", defaultValue = "de") String lang
    ) {
        return service.adminList(status, page, size, sortBy, sortDir, lang);
    }

    @GetMapping("/{id:[0-9a-fA-F\\-]{36}}")
    @Operation(summary = "Get order by id for admin")
    public OrderResponse getOne(
            @PathVariable UUID id,
            @RequestHeader(name = "Accept-Language", defaultValue = "de") String lang
    ) {
        return service.getById(id, lang);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status")
    public OrderResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest req,
            @RequestHeader(name = "Accept-Language", defaultValue = "de") String lang
    ) {
        return service.adminUpdateStatus(id, req.status(), lang);
    }

//    @GetMapping("/{id}")
//    public Order get(@PathVariable UUID id) {
//        return orderRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
//    }
//

//    Note: In practice, updating paymentStatus via Stripe/PayPal webhook is the "correct" approach,
//    while the admin endpoint is for handling exceptions.
//    @PatchMapping("/{id}/payment-status")
//    public Order updatePaymentStatus(@PathVariable UUID id, @RequestBody UpdatePaymentStatusRequest req) {
//        Order o = orderRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
//        o.setPaymentStatus(req.status());
//        return orderRepo.save(o);
//    }
}
