package de.thfamily18.restaurant_backend.entity;

public enum RefundStatus {
    PENDING,
    REQUIRES_ACTION,
    SUCCEEDED,
    FAILED,
    CANCELED;

    public static RefundStatus fromStripe(String status) {
        if (status == null) return null;

        return switch (status.toLowerCase()) {
            case "pending" -> PENDING;
            case "requires_action" -> REQUIRES_ACTION;
            case "succeeded" -> SUCCEEDED;
            case "failed" -> FAILED;
            case "canceled" -> CANCELED;
            default -> throw new IllegalArgumentException("Unknown refund status: " + status);
        };
    }
}

