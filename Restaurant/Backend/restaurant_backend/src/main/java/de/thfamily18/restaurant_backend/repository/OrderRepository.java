package de.thfamily18.restaurant_backend.repository;

import de.thfamily18.restaurant_backend.entity.Order;
import de.thfamily18.restaurant_backend.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findAllByOrderStatus(OrderStatus status, Pageable pageable);
    Page<Order> findAllByUser_Id(UUID userId, Pageable pageable);
}
