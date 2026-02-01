package de.thfamily18.restaurant_backend.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Lock a batch of due notifications using Postgres SKIP LOCKED.
     *
     * Also re-claim notifications stuck in SENDING for too long (worker crashed).
     * We treat them as "timed-out processing" and allow another worker to claim them.
     *
     * NOTE: This is "at-least-once" delivery. Your email sending should be tolerant
     * to duplicates, or you should implement idempotency if needed later.
     */
    @Query(value = """
        select *
        from notifications
        where
            (
                status in ('PENDING','FAILED')
                and next_attempt_at <= :now
            )
            or
            (
                status = 'SENDING'
                and processing_started_at is not null
                and processing_started_at <= :sendingTimeout
            )
        order by created_at asc
        limit :limit
        for update skip locked
        """, nativeQuery = true)
    List<Notification> lockNextReady(
            @Param("now") LocalDateTime now,
            @Param("sendingTimeout") LocalDateTime sendingTimeout,
            @Param("limit") int limit
    );

    /**
     * Update notification state after sending attempt.
     *
     * - On SENT: set sentAt, clear lastError, clear processingStartedAt
     * - On FAILED: set lastError, schedule retry, clear processingStartedAt, increment attempts
     *
     * This method is optional; in this solution we update entity in Tx#2 for clarity.
     * Keep it if you prefer bulk/atomic update via JPQL.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update Notification n
       set n.status = :status,
           n.lastError = :lastError,
           n.sentAt = :sentAt,
           n.nextAttemptAt = :nextAttemptAt,
           n.attempts = :attempts,
           n.processingStartedAt = :processingStartedAt
     where n.id = :id
""")
    int updateAfterSend(
            @Param("id") UUID id,
            @Param("status") NotificationStatus status,
            @Param("lastError") String lastError,
            @Param("sentAt") LocalDateTime sentAt,
            @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
            @Param("attempts") int attempts,
            @Param("processingStartedAt") LocalDateTime processingStartedAt
    );
}
