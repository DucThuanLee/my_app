package de.thfamily18.restaurant_backend.dto.payment;

public record CreateStripeIntentResponse(String paymentIntentId, String clientSecret) {}
