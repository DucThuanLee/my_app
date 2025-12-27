package de.thfamily18.restaurant_backend.controller;

import de.thfamily18.restaurant_backend.dto.OrderResponse;
import de.thfamily18.restaurant_backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// User orders
@RestController
@RequestMapping("/api/user/orders")
@RequiredArgsConstructor
public class UserOrderController {

    private final OrderService service;

    @GetMapping
    public Page<OrderResponse> myOrders(
            Authentication auth,
            @RequestHeader(name="Accept-Language", defaultValue="de") String lang,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) {

        return service.getOrdersForUser(auth.getName(), lang, page, size);
    }
}
