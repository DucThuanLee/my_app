package de.thfamily18.restaurant_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name="orders")
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
    @JoinColumn(name="user_id")
    private User user; // guest => null

    // GDPR: only store what is necessary for delivery
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
    // ===== Stripe Payment =====
    @Column(name="stripe_payment_intent_id", unique = true)
    private String stripePaymentIntentId;

    private LocalDateTime paidAt;
    // ===== Stripe Refund =====
    @Column(name = "stripe_refund_id", unique = true)
    private String stripeRefundId;
    private LocalDateTime refundRequestedAt;
    private LocalDateTime refundedAt;
    // Optional: store requested/refunded amount (useful for partial refunds)
    @Column(precision = 12, scale = 2)
    private BigDecimal refundedAmount;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (orderStatus == null) orderStatus = OrderStatus.NEW;
        if (paymentStatus == null) paymentStatus = PaymentStatus.PENDING;
        if (items == null) items = new ArrayList<>();
    }

    public void addItem(OrderItem item) {
        if (items == null) items = new ArrayList<>();
        items.add(item);
        item.setOrder(this);
    }
}
