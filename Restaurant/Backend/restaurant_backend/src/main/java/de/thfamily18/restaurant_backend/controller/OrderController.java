package de.thfamily18.restaurant_backend.controller;

import de.thfamily18.restaurant_backend.dto.CreateOrderRequest;
import de.thfamily18.restaurant_backend.dto.OrderResponse;
import de.thfamily18.restaurant_backend.entity.OrderStatus;
import de.thfamily18.restaurant_backend.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Guest create
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
// GET /api/orders/me?page=0&size=10
//GET /api/orders/me?status=NEW&page=0&size=10
//GET /api/orders/me?status=DELIVERED&page=1&size=5&sortBy=totalPrice&sortDir=asc
public class OrderController {

    private final OrderService service;

    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody CreateOrderRequest req,
            @RequestHeader(name = "Accept-Language", defaultValue = "de") String lang,
            Authentication auth
    ) {
        // Guest vs Logged-in
        boolean loggedIn = auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);

        OrderResponse res = loggedIn
                ? service.createUserOrder(auth.getName(), req, lang)
                : service.createGuestOrder(req, lang);

        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get my orders",
            description = "Returns the current user's orders with optional status filter, pagination, and sorting."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @Parameter(
                    description = "Optional filter by order status",
                    example = "NEW"
            )
            @RequestParam(required = false) OrderStatus status,

            @Parameter(
                    description = "Page index (0-based)",
                    example = "0"
            )
            @RequestParam(defaultValue = "0") int page,

            @Parameter(
                    description = "Page size",
                    example = "20"
            )
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

            @Parameter(
                    description = "Response language",
                    example = "de"
            )
            @RequestHeader(name = "Accept-Language", defaultValue = "de") String lang,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                service.getMyOrders(authentication.getName(), status, page, size, sortBy, sortDir, lang)
        );
    }

    @GetMapping("/{id:[0-9a-fA-F\\-]{36}}")
    public ResponseEntity<OrderResponse> getById(
            @PathVariable UUID id,
            @RequestHeader(name = "Accept-Language", defaultValue = "de") String lang
    ) {
        return ResponseEntity.ok(service.getById(id, lang));
    }
}
