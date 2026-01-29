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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Notification enqueuePaymentSucceeded(UUID orderId, String email, Map<String, Object> vars) {
        return repo.save(Notification.builder()
                .type(NotificationType.PAYMENT_SUCCEEDED)
                .recipient(email)
                .orderId(orderId)
                .status(NotificationStatus.PENDING)
                .nextAttemptAt(LocalDateTime.now())
                .payload(toJson(vars))
                .build());
    }

    public Notification enqueueRefundSucceeded(UUID orderId, String email, Map<String, Object> vars) {
        return repo.save(Notification.builder()
                .type(NotificationType.REFUND_SUCCEEDED)
                .recipient(email)
                .orderId(orderId)
                .status(NotificationStatus.PENDING)
                .nextAttemptAt(LocalDateTime.now())
                .payload(toJson(vars))
                .build());
    }

    private String toJson(Map<String, Object> vars) {
        try {
            return (vars == null || vars.isEmpty()) ? null : objectMapper.writeValueAsString(vars);
        } catch (Exception e) {
            // nếu serialize fail thì vẫn cho enqueue, payload null
            return null;
        }
    }
}