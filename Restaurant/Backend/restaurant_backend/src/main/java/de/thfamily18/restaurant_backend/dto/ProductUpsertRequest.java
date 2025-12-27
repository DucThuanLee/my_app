package de.thfamily18.restaurant_backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// Admin create/update
public record ProductUpsertRequest(
        @NotBlank String nameDe,
        @NotBlank String nameEn,
        String descriptionDe,
        String descriptionEn,
        @NotNull @DecimalMin(value="0.0", inclusive=false) BigDecimal price,
        @NotBlank String category,
        boolean bestSeller
) {}
