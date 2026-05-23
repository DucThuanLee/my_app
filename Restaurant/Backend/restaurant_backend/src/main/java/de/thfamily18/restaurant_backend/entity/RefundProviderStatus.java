package de.thfamily18.restaurant_backend.entity;

public enum RefundProviderStatus {

    PENDING,
    REQUIRES_ACTION,
    SUCCEEDED,
    FAILED,
    CANCELED;
    public static RefundProviderStatus fromProvider(String status) {

        if (status == null || status.isBlank()) {
            return null;
        }

        return switch (status.toLowerCase()) {
            case "pending" -> PENDING;
            case "requires_action" -> REQUIRES_ACTION;
            case "completed",
                 "succeeded",
                 "success" -> SUCCEEDED;
            case "failed",
                 "denied",
                 "declined" -> FAILED;
            case "canceled",
                 "cancelled" -> CANCELED;
            default -> throw new IllegalArgumentException(
                    "Unknown refund status: " + status
            );
        };
    }
}

