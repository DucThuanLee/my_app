package de.thfamily18.restaurant_backend.repository;

import de.thfamily18.restaurant_backend.entity.Refund;
import de.thfamily18.restaurant_backend.entity.RefundProviderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, UUID> {
    /**
     * Find single refund by Stripe refund id (idempotency check)
     */
    Optional<Refund> findByStripeRefundId(String stripeRefundId);

    /**
     * Batch query to avoid N+1 problem
     */
    List<Refund> findAllByStripeRefundIdIn(List<String> stripeRefundIds);

    /**
     * Get all refunds of an order
     */
    List<Refund> findAllByOrderId(UUID orderId);

    /**
     * Sum ONLY succeeded refunds (CRITICAL for correctness)
     */
    @Query("""
        SELECT COALESCE(SUM(r.amount), 0)
        FROM Refund r
        WHERE r.order.id = :orderId
          AND r.status = 'SUCCEEDED'
    """)
    BigDecimal sumSucceededAmountByOrderId(UUID orderId);

    @Query("""
        select coalesce(sum(r.amount), 0)
        from Refund r
        where r.order.id = :orderId
          and r.status = :status
    """)
    BigDecimal sumAmountByOrderIdAndStatus(
            @Param("orderId") UUID orderId,
            @Param("status") RefundProviderStatus status
    );

    /**
     * Optional: get latest refund (useful for UI/debug)
     */
    Optional<Refund> findTopByOrderIdOrderByCreatedAtDesc(UUID orderId);

    /**
     * Optional: filter by status
     */
    List<Refund> findAllByOrderIdAndStatus(UUID orderId, RefundProviderStatus status);

    Optional<Refund> findByPaypalRefundId(String paypalRefundId);
}
