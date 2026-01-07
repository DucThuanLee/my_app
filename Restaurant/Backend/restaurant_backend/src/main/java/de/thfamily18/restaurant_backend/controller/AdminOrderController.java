package de.thfamily18.restaurant_backend.controller;

// Admin manage orders

import de.thfamily18.restaurant_backend.dto.OrderResponse;
import de.thfamily18.restaurant_backend.dto.UpdateOrderStatusRequest;
import de.thfamily18.restaurant_backend.entity.OrderStatus;
import de.thfamily18.restaurant_backend.service.OrderService;
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
public class AdminOrderController {

    private final OrderService service;

    @GetMapping
    public Page<OrderResponse> list(
            @RequestParam(required=false) OrderStatus status,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size,
            @RequestHeader(name="Accept-Language", defaultValue="de") String lang) {

        return service.adminList(status, page, size, lang);
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest req,
            @RequestHeader(name="Accept-Language", defaultValue="de") String lang) {

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
