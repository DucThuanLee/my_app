package de.thfamily18.restaurant_backend.notification;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications",
        indexes = {
                @Index(name="idx_notifications_status_next", columnList = "status,next_attempt_at"),
                @Index(name="idx_notifications_order", columnList = "order_id")
        })
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationChannel channel = NotificationChannel.EMAIL;

    // email, phone, deviceToken... (hiện tại chỉ EMAIL)
    @Column(nullable = false, length = 320)
    private String recipient;

    // Liên kết tới domain object để build nội dung (ví dụ order receipt/refund)
    @Column(name = "order_id")
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "next_attempt_at", nullable = false)
    @Builder.Default
    private LocalDateTime nextAttemptAt = LocalDateTime.now();

    // Lưu lỗi gần nhất (ngắn thôi)
    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    // Optional: metadata dạng JSON string (variables cho template)
    // Nếu chưa muốn JSON column thì để TEXT.
    @Column(name="payload", columnDefinition = "text")
    private String payload;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (nextAttemptAt == null) nextAttemptAt = LocalDateTime.now();
        if (status == null) status = NotificationStatus.PENDING;
        if (channel == null) channel = NotificationChannel.EMAIL;
    }
}
