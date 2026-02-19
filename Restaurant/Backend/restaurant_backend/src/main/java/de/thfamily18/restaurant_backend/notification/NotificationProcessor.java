package de.thfamily18.restaurant_backend.notification;

import de.thfamily18.restaurant_backend.notification.mail.EmailSender;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
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

    // For deterministic tests and consistent timestamps
    private final Clock clock;

    // Dead-letter: after this many failed attempts, stop retrying
    @Value("${notification.maxAttempts:10}")
    private int maxAttempts;

    // Micrometer
    private final MeterRegistry meterRegistry;

    private Counter sentCounter;
    private Counter failedCounter;
    private Counter deadCounter;
    private Timer sendTimer;

    @PostConstruct
    void initMetrics() {
        this.sentCounter = meterRegistry.counter("notifications.sent");
        this.failedCounter = meterRegistry.counter("notifications.failed");
        this.deadCounter = meterRegistry.counter("notifications.dead");
        this.sendTimer = meterRegistry.timer("notifications.send.time");
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * Poll due notifications and process them.
     * The email sending is intentionally OUTSIDE transaction.
     */
    @Scheduled(fixedDelay = 5000)
    public void processDue() {
        List<SendTask> tasks = claimBatch();
        if (tasks.isEmpty()) return;

        for (SendTask t : tasks) {
            Timer.Sample sample = Timer.start(meterRegistry);

            try {
                // 1) Outside transaction: external side effect
                emailSender.send(t.recipient(), subjectOf(t), bodyOf(t));

                // 2) Tx #2: mark SENT
                markSent(t.notificationId());

                sentCounter.increment();
            } catch (Exception ex) {
                log.error("Send notification failed id={} type={} to={}",
                        t.notificationId(), t.type(), t.recipient(), ex);

                // Tx #2: mark FAILED or DEAD + backoff
                markFailed(t.notificationId(), ex);
            } finally {
                sample.stop(sendTimer);
            }
        }
    }

    /**
     * Tx #1: lock a batch and mark them as SENDING (PROCESSING).
     */
    private List<SendTask> claimBatch() {
        LocalDateTime now = now();
        LocalDateTime sendingTimeout = now.minusMinutes(SENDING_TIMEOUT_MINUTES);

        return tx.execute(status -> {
            List<Notification> locked = repo.lockNextReady(now, sendingTimeout, BATCH_SIZE);
            if (locked.isEmpty()) return List.of();

            List<SendTask> out = new ArrayList<>(locked.size());
            for (Notification n : locked) {
                n.setStatus(NotificationStatus.SENDING);
                n.setProcessingStartedAt(now());
                n.setLastError(null);

                out.add(new SendTask(
                        n.getId(),
                        n.getRecipient(),
                        n.getType(),
                        n.getOrderId(),
                        n.getPayload(),
                        n.getAttempts()
                ));
            }
            return out;
        });
    }

    /**
     * Tx #2: mark SENT.
     * Keep attempts/nextAttemptAt as-is. Clear processingStartedAt.
     */
    void markSent(UUID id) {
        tx.executeWithoutResult(status -> {
            Notification n = repo.findById(id).orElse(null);
            if (n == null) return;

            repo.updateAfterSend(
                    id,
                    NotificationStatus.SENT,
                    null,               // lastError
                    now(),              // sentAt
                    n.getNextAttemptAt(),// keep as-is
                    n.getAttempts(),     // keep as-is
                    null                 // clear processingStartedAt
            );
        });
    }

    /**
     * Tx #2: mark FAILED, increment attempts, schedule retry.
     * If attempts >= maxAttempts -> DEAD (dead-letter).
     */
    void markFailed(UUID id, Exception ex) {
        tx.executeWithoutResult(status -> {
            Notification n = repo.findById(id).orElse(null);
            if (n == null) return;

            int attempts = n.getAttempts() + 1;
            String err = NotificationRetryPolicy.trim(ex.getMessage(), 500);

            if (attempts >= maxAttempts) {
                n.setAttempts(attempts);
                n.setStatus(NotificationStatus.DEAD);
                n.setLastError(err);
                n.setProcessingStartedAt(null);
                n.setNextAttemptAt(null);     // no more retries
                n.setDeadLetteredAt(now());

                deadCounter.increment();
                return;
            }

            n.setAttempts(attempts);
            n.setStatus(NotificationStatus.FAILED);
            n.setLastError(err);
            n.setProcessingStartedAt(null);
            n.setNextAttemptAt(now().plusSeconds(NotificationRetryPolicy.backoffSeconds(attempts)));

            failedCounter.increment();
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
                + "\nTime: " + now()
                + (t.payload() != null ? "\nPayload: " + t.payload() : "");
    }

    public record SendTask(
            UUID notificationId,
            String recipient,
            NotificationType type,
            UUID orderId,
            String payload,
            int attempts
    ) {}
}