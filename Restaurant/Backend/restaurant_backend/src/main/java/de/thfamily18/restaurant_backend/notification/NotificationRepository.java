package de.thfamily18.restaurant_backend.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Lock a batch of due notifications using Postgres SKIP LOCKED.
     * This prevents multiple workers from processing the same row.
     */
    @Query(value = """
        select *
        from notifications
        where status in ('PENDING','FAILED')
          and next_attempt_at <= :now
        order by created_at asc
        limit :limit
        for update skip locked
        """, nativeQuery = true)
    List<Notification> lockNextReady(LocalDateTime now, int limit);

    @Modifying
    @Query("""
        update Notification n
           set n.status = :status,
               n.lastError = :lastError,
               n.sentAt = :sentAt,
               n.nextAttemptAt = :nextAttemptAt,
               n.attempts = :attempts
         where n.id = :id
    """)
    int updateAfterSend(
            UUID id,
            NotificationStatus status,
            String lastError,
            LocalDateTime sentAt,
            LocalDateTime nextAttemptAt,
            int attempts
    );
}
