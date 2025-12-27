package de.thfamily18.restaurant_backend.dto;

import de.thfamily18.restaurant_backend.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        // guest: required for delivery, user: still ok
        @NotBlank String customerName,
        @NotBlank String phone,
        @NotBlank String address,

        @NotNull PaymentMethod paymentMethod,

        @NotEmpty @Valid List<OrderItemRequest> items
) {}
