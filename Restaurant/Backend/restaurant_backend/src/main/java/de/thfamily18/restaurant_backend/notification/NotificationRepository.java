package de.thfamily18.restaurant_backend.notification;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Get jobs that are due to be submitted (lock them to prevent multiple instances from submitting the same job).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select n from Notification n
           where n.status in ('PENDING','FAILED')
             and n.nextAttemptAt <= :now
           order by n.createdAt asc
           """)
    // Pageable???
    List<Notification> findDue(LocalDateTime now);
}
