package de.thfamily18.restaurant_backend.dto.payment;

import java.util.UUID;

public record CreateStripeIntentRequest(UUID orderId) {}
