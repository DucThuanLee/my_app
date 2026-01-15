package de.thfamily18.restaurant_backend.dto;

import de.thfamily18.restaurant_backend.entity.OrderStatus;
import de.thfamily18.restaurant_backend.entity.PaymentMethod;
import de.thfamily18.restaurant_backend.entity.PaymentStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record OrderResponse(
        UUID id,
//        String customerName,
//        String phone,
//        String address,
        BigDecimal totalPrice,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        OrderStatus orderStatus,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {}
