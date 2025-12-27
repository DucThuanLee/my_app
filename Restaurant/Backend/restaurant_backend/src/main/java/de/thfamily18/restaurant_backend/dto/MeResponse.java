package de.thfamily18.restaurant_backend.dto;

import de.thfamily18.restaurant_backend.entity.Role;

public record MeResponse(String email, Role role) {}