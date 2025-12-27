package de.thfamily18.restaurant_backend.dto;

import de.thfamily18.restaurant_backend.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

// for admin
public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {}
