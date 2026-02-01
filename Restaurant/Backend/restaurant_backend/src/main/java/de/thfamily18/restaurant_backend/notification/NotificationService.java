package de.thfamily18.restaurant_backend.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repo;
    private final ObjectMapper objectMapper;

    /**
     * Enqueue a notification job (persist to DB).
     * This method MUST NOT send emails or call external services.
     */
    public Notification enqueue(
            NotificationType type,
            NotificationChannel channel,
            UUID orderId,
            String recipient,
            Map<String, Object> vars
    ) {
        Notification n = Notification.builder()
                .type(type)
                .channel(channel == null ? NotificationChannel.EMAIL : channel)
                .recipient(recipient)
                .orderId(orderId)
                .status(NotificationStatus.PENDING)
                .attempts(0)
                .nextAttemptAt(LocalDateTime.now())
                .payload(toJson(vars))
                .build();

        return repo.save(n);
    }

    public Notification enqueuePaymentSucceeded(UUID orderId, String email, Map<String, Object> vars) {
        return enqueue(NotificationType.PAYMENT_SUCCEEDED, NotificationChannel.EMAIL, orderId, email, vars);
    }

    public Notification enqueueRefundSucceeded(UUID orderId, String email, Map<String, Object> vars) {
        return enqueue(NotificationType.REFUND_SUCCEEDED, NotificationChannel.EMAIL, orderId, email, vars);
    }

    private String toJson(Map<String, Object> vars) {
        if (vars == null || vars.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(vars);
        } catch (Exception ignored) {
            // If serialization fails, still enqueue the job with null payload.
            return null;
        }
    }
}