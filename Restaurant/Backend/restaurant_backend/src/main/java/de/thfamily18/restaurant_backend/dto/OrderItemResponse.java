package de.thfamily18.restaurant_backend.dto;

import lombok.Builder;
import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record OrderItemResponse(
        UUID productId,
        String productName,
        int quantity,
        BigDecimal price
) {}