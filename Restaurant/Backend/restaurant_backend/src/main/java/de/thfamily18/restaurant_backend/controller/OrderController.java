package de.thfamily18.restaurant_backend.controller;

import de.thfamily18.restaurant_backend.dto.CreateOrderRequest;
import de.thfamily18.restaurant_backend.dto.OrderResponse;
import de.thfamily18.restaurant_backend.service.OrderService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Guest create
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
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
}
