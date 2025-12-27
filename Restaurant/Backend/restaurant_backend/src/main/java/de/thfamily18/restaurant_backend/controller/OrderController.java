package de.thfamily18.restaurant_backend.controller;

import de.thfamily18.restaurant_backend.dto.CreateOrderRequest;
import de.thfamily18.restaurant_backend.dto.OrderResponse;
import de.thfamily18.restaurant_backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            @RequestHeader(name="Accept-Language", defaultValue="de") String lang) {

        OrderResponse created = service.createGuestOrder(req, lang);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
