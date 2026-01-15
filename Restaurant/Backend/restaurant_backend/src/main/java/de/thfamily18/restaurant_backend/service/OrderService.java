package de.thfamily18.restaurant_backend.service;

import de.thfamily18.restaurant_backend.dto.CreateOrderRequest;
import de.thfamily18.restaurant_backend.dto.CreateOrderItemRequest;
import de.thfamily18.restaurant_backend.dto.OrderItemResponse;
import de.thfamily18.restaurant_backend.dto.OrderResponse;
import de.thfamily18.restaurant_backend.entity.*;
import de.thfamily18.restaurant_backend.exception.ResourceNotFoundException;
import de.thfamily18.restaurant_backend.repository.OrderRepository;
import de.thfamily18.restaurant_backend.repository.ProductRepository;
import de.thfamily18.restaurant_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    // ===== Public / User =====

    public OrderResponse createGuestOrder(CreateOrderRequest req, String langHeader) {
        String lang = normalizeLang(langHeader);
        Order order = buildOrder(null, req);
        return toResponse(orderRepo.save(order), lang);
    }

    public OrderResponse createUserOrder(String email, CreateOrderRequest req, String langHeader) {
        String lang = normalizeLang(langHeader);

        User u = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = buildOrder(u, req);
        return toResponse(orderRepo.save(order), lang);
    }

    public Page<OrderResponse> getOrdersForUser(String email, String langHeader, int page, int size) {
        String lang = normalizeLang(langHeader);

        User u = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Page<Order> orders = orderRepo.findAllByUser_Id(
                u.getId(),
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        return orders.map(o -> toResponse(o, lang));
    }

    // ===== Admin =====

    public Page<OrderResponse> adminList(OrderStatus status, int page, int size, String langHeader) {
        String lang = normalizeLang(langHeader);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders = (status == null)
                ? orderRepo.findAll(pageable)
                : orderRepo.findAllByOrderStatus(status, pageable);

        return orders.map(o -> toResponse(o, lang));
    }

    public OrderResponse adminUpdateStatus(UUID id, OrderStatus status, String langHeader) {
        String lang = normalizeLang(langHeader);

        if (status == null) {
            throw new IllegalArgumentException("Order status must not be null");
        }

        Order o = orderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        o.setOrderStatus(status);
        return toResponse(orderRepo.save(o), lang);
    }

    // ===== Core builder =====

    private Order buildOrder(User user, CreateOrderRequest req) {
        Order o = Order.builder()
                .user(user) // nullable -> guest
                .customerName(req.customerName())
                .phone(req.phone())
                .address(req.address())
                .paymentMethod(req.paymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .orderStatus(OrderStatus.NEW)
                .totalPrice(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now()) // Avoid null when builder
                .items(new ArrayList<>())
                .build();

        BigDecimal total = BigDecimal.ZERO;

        // IMPORTANT: ensure order-items relation is set correctly
        o.getItems().clear();

        for (CreateOrderItemRequest itemReq : req.items()) {
            Product p = productRepo.findById(itemReq.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemReq.productId()));

            int qty = itemReq.quantity();
            BigDecimal unitPrice = p.getPrice();
            BigDecimal line = unitPrice.multiply(BigDecimal.valueOf(qty));
            total = total.add(line);

            OrderItem oi = OrderItem.builder()
                    .order(o)          // set FK
                    .product(p)
                    .quantity(qty)
                    .price(unitPrice)  // snapshot price at order time
                    .build();

            // add to order collection
            o.getItems().add(oi);
        }

        o.setTotalPrice(total);
        return o;
    }

    // ===== Mapping =====

    private OrderResponse toResponse(Order o, String lang) {
        boolean de = lang != null && lang.toLowerCase().startsWith("de");

        List<OrderItemResponse> items = o.getItems().stream()
                .map(oi -> OrderItemResponse.builder()
                        .productId(oi.getProduct().getId())
                        .productName(de ? oi.getProduct().getNameDe() : oi.getProduct().getNameEn())
                        .quantity(oi.getQuantity())
                        .price(oi.getPrice())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(o.getId())
                .totalPrice(o.getTotalPrice())
                .paymentMethod(o.getPaymentMethod())
                .paymentStatus(o.getPaymentStatus())
                .orderStatus(o.getOrderStatus())
                .createdAt(o.getCreatedAt())
                .items(items)
                .build();
    }

    // ===== Utils =====

    private String normalizeLang(String langHeader) {
        if (langHeader == null || langHeader.isBlank()) return "de";
        String first = langHeader.split(",")[0].trim();     // "de-DE"
        String base = first.split("-")[0].trim().toLowerCase(); // "de"
        return base.equals("en") ? "en" : "de";
    }
}

