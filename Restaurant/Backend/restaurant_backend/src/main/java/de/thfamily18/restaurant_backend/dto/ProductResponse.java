package de.thfamily18.restaurant_backend.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        String category,
        boolean bestSeller
) {
}
