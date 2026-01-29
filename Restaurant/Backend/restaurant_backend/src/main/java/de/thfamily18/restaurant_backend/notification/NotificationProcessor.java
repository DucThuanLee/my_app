package de.thfamily18.restaurant_backend.notification;

import de.thfamily18.restaurant_backend.notification.mail.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProcessor {

    private final NotificationRepository repo;
    @Qualifier("logEmailSender")
    private final EmailSender emailSender;

    // Scan every 5 seconds (dev). Prod can scan every 10-30 seconds.
    @Scheduled(fixedDelay = 5000)
    public void processDue() {
        List<Notification> due = repo.findDue(LocalDateTime.now());
        if (due.isEmpty()) return;

        for (Notification n : due) {
            try {
                n.setStatus(NotificationStatus.SENDING);
                repo.save(n);

                // Build the minimum email content (template it later).
                String subject = subjectOf(n);
                String body = bodyOf(n);

                emailSender.send(n.getRecipient(), subject, body);

                n.setStatus(NotificationStatus.SENT);
                n.setSentAt(LocalDateTime.now());
                n.setLastError(null);
                repo.save(n);

            } catch (Exception ex) {
                log.error("Send notification failed id={} type={} to={}", n.getId(), n.getType(), n.getRecipient(), ex);

                int attempts = n.getAttempts() + 1;
                n.setAttempts(attempts);
                n.setStatus(NotificationStatus.FAILED);
                n.setLastError(trim(ex.getMessage(), 500));

                // backoff đơn giản: 10s, 30s, 2m, 10m...
                n.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoffSeconds(attempts)));

                repo.save(n);
            }
        }
    }

    private String subjectOf(Notification n) {
        return switch (n.getType()) {
            case PAYMENT_SUCCEEDED -> "Payment received";
            case REFUND_SUCCEEDED -> "Refund processed";
            case REFUND_REQUESTED -> "Refund requested";
            case ORDER_CREATED -> "Order received";
        };
    }

    private String bodyOf(Notification n) {
        // tối thiểu cho test
        return "Notification type: " + n.getType()
                + "\nOrderId: " + n.getOrderId()
                + "\nTime: " + LocalDateTime.now()
                + (n.getPayload() != null ? "\nPayload: " + n.getPayload() : "");
    }

    private long backoffSeconds(int attempts) {
        return switch (attempts) {
            case 1 -> 10;
            case 2 -> 30;
            case 3 -> 120;
            default -> 600;
        };
    }

    private String trim(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}