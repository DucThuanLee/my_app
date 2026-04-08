package de.thfamily18.restaurant_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "refunds",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_stripe_refund_id", columnNames = "stripe_refund_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund {

    @Id
    @GeneratedValue
    private UUID id;

    // ===== RELATION =====
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // ===== STRIPE =====
    @Column(name = "stripe_refund_id", nullable = false, updatable = false)
    private String stripeRefundId;

    @Column(name = "stripe_charge_id")
    private String stripeChargeId;

    // ===== MONEY =====
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    // ===== STATUS =====
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StripeRefundStatus status;

    // ===== OPTIONAL =====
    @Column(length = 50)
    private String reason; // duplicate, fraud, requested_by_customer

    @Column(length = 255)
    private String failureReason;

    // ===== AUDIT =====
    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ===== LIFECYCLE =====
    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}