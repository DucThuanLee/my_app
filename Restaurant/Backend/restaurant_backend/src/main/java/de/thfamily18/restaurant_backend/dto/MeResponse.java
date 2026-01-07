package de.thfamily18.restaurant_backend.dto;

import de.thfamily18.restaurant_backend.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current user information")
public record MeResponse(
        @Schema(example = "admin@shop.de")
        String email,
        @Schema(example = "ADMIN")
        Role role) {}