package de.thfamily18.restaurant_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // guest => null

    // ===== Customer info (GDPR minimal) =====
    private String customerName;
    private String phone;
    private String address;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.NEW;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    LocalDateTime updatedAt;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "EUR";

    // ================= STRIPE PAYMENT =================
    @Column(name = "stripe_payment_intent_id", unique = true)
    private String stripePaymentIntentId;

    private LocalDateTime paidAt;

    // ================= REFUND (BUSINESS STATE) =================
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RefundStatus refundStatus = RefundStatus.NOT_REQUESTED;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Refund> refunds = new ArrayList<>();

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    private LocalDateTime refundedAt;

    // ================= LIFECYCLE =================

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (orderStatus == null) orderStatus = OrderStatus.NEW;
        if (paymentStatus == null) paymentStatus = PaymentStatus.PENDING;
        if (items == null) items = new ArrayList<>();
        if (refundedAmount == null) refundedAmount = BigDecimal.ZERO;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addItem(OrderItem item) {
        if (items == null) items = new ArrayList<>();
        items.add(item);
        item.setOrder(this);
    }

    // ================= BUSINESS HELPERS =================

    public boolean isFullyRefunded() {
        return refundedAmount != null
                && totalPrice != null
                && refundedAmount.compareTo(totalPrice) >= 0;
    }

    public boolean isPartiallyRefunded() {
        return refundedAmount != null
                && refundedAmount.compareTo(BigDecimal.ZERO) > 0
                && refundedAmount.compareTo(totalPrice) < 0;
    }
}
