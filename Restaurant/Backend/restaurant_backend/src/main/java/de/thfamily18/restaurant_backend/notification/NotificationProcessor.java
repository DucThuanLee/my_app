package de.thfamily18.restaurant_backend.notification;

import de.thfamily18.restaurant_backend.notification.mail.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProcessor {

    private static final int BATCH_SIZE = 20;
    /**
     * If a notification stays in SENDING longer than this,
     * we assume the worker crashed and allow re-claim.
     */
    private static final int SENDING_TIMEOUT_MINUTES = 5;
    private final NotificationRepository repo;
    private final TransactionTemplate tx;
    private final EmailSender emailSender;


    /**
     * Poll due notifications and process them.
     * The email sending is intentionally OUTSIDE transaction.
     */
    @Scheduled(fixedDelay = 5000)
    public void processDue() {
        List<SendTask> tasks = claimBatch();
        if (tasks.isEmpty()) return;

        for (SendTask t : tasks) {
            try {
                // 1) Outside transaction: do the external side effect
                emailSender.send(t.recipient(), subjectOf(t), bodyOf(t));

                // 2) Tx #2: mark SENT
                markSent(t);

            } catch (Exception ex) {
                log.error("Send notification failed id={} type={} to={}",
                        t.notificationId(), t.type(), t.recipient(), ex);

                // Tx #2: mark FAILED + backoff
                markFailed(t.notificationId(), ex);
            }
        }
    }

    /**
     * Tx #1: lock a batch and mark them as SENDING (PROCESSING).
     */
    private List<SendTask> claimBatch() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sendingTimeout = now.minusMinutes(SENDING_TIMEOUT_MINUTES);
        return tx.execute(status -> {
            List<Notification> locked = repo.lockNextReady(now, sendingTimeout, BATCH_SIZE);
            if (locked.isEmpty()) return List.of();

            List<SendTask> out = new ArrayList<>(locked.size());
            for (Notification n : locked) {
                n.setStatus(NotificationStatus.SENDING); // "PROCESSING"
                n.setProcessingStartedAt(LocalDateTime.now());
                n.setLastError(null);
                // Optional: increment attempts when actually trying (some teams do it here)
                // n.setAttempts(n.getAttempts() + 1);

                out.add(new SendTask(
                        n.getId(),
                        n.getRecipient(),
                        n.getType(),
                        n.getOrderId(),
                        n.getPayload(),
                        n.getAttempts()
                ));
            }
            // JPA dirty-check will flush on commit
            return out;
        });
    }

    /**
     * Tx #2: mark SENT.
     *
     * We keep `attempts` as-is (taken from the claimed task),
     * clear `processingStartedAt`, and set `nextAttemptAt` to NULL because
     * a successfully sent notification should not be retried.
     */
    private void markSent(SendTask t) {
        tx.executeWithoutResult(status -> {
            repo.updateAfterSend(
                    t.notificationId(),
                    NotificationStatus.SENT,
                    null,
                    LocalDateTime.now(),
                    null, // no retry needed after success
                    t.attempts(),
                    null  // clear processing marker
            );
        });
    }

    /**
     * Tx #2: mark FAILED, increment attempts and schedule retry.
     */
    private void markFailed(UUID id, Exception ex) {
        tx.executeWithoutResult(status -> {
            Notification n = repo.findById(id).orElse(null);
            if (n == null) return;

            int attempts = n.getAttempts() + 1;

            n.setAttempts(attempts);
            n.setStatus(NotificationStatus.FAILED);
            n.setLastError(NotificationRetryPolicy.trim(ex.getMessage(), 500));
            // Clear processing mark so it can be claimed again later
            n.setProcessingStartedAt(null);
            n.setNextAttemptAt(LocalDateTime.now().plusSeconds(NotificationRetryPolicy.backoffSeconds(attempts)));
        });
    }

    private String subjectOf(SendTask t) {
        return switch (t.type()) {
            case PAYMENT_SUCCEEDED -> "Payment received";
            case REFUND_SUCCEEDED -> "Refund processed";
            case REFUND_REQUESTED -> "Refund requested";
            case ORDER_CREATED -> "Order received";
        };
    }

    private String bodyOf(SendTask t) {
        return "Notification type: " + t.type()
                + "\nOrderId: " + t.orderId()
                + "\nTime: " + LocalDateTime.now()
                + (t.payload() != null ? "\nPayload: " + t.payload() : "");
    }

    /**
     * Minimal data needed outside transaction.
     * Never pass entity objects outside tx.
     */
    public record SendTask(
            java.util.UUID notificationId,
            String recipient,
            NotificationType type,
            java.util.UUID orderId,
            String payload,
            int attempts
    ) {}
}