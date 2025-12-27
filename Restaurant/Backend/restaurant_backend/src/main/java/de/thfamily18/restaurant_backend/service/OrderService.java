package de.thfamily18.restaurant_backend.service;

import de.thfamily18.restaurant_backend.dto.CreateOrderRequest;
import de.thfamily18.restaurant_backend.dto.OrderItemRequest;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    public OrderResponse createGuestOrder(CreateOrderRequest req, String lang) {
        Order order = buildOrder(null, req);
        return toResponse(orderRepo.save(order), lang);
    }

    public Page<OrderResponse> getOrdersForUser(String email, String lang, int page, int size) {
        User u = userRepo.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Page<Order> orders = orderRepo.findAllByUser_Id(u.getId(), PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return orders.map(o -> toResponse(o, lang));
    }

    public Page<OrderResponse> adminList(OrderStatus status, int page, int size, String lang) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders = (status == null) ? orderRepo.findAll(pageable) : orderRepo.findAllByOrderStatus(status, pageable);
        return orders.map(o -> toResponse(o, lang));
    }

    public OrderResponse adminUpdateStatus(UUID id, OrderStatus status, String lang) {
        Order o = orderRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        o.setOrderStatus(status);
        return toResponse(orderRepo.save(o), lang);
    }

    private Order buildOrder(User user, CreateOrderRequest req) {
        Order o = Order.builder()
                .user(user)
                .customerName(req.customerName())
                .phone(req.phone())
                .address(req.address())
                .paymentMethod(req.paymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .orderStatus(OrderStatus.NEW)
                .totalPrice(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();

        for (OrderItemRequest itemReq : req.items()) {
            Product p = productRepo.findById(itemReq.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            BigDecimal line = p.getPrice().multiply(BigDecimal.valueOf(itemReq.quantity()));
            total = total.add(line);

            OrderItem oi = OrderItem.builder()
                    .order(o)
                    .product(p)
                    .quantity(itemReq.quantity())
                    .price(p.getPrice())
                    .build();
            items.add(oi);
        }

        o.setTotalPrice(total);
        o.getItems().clear();
        o.getItems().addAll(items);
        return o;
    }

    private OrderResponse toResponse(Order o, String lang) {
        boolean de = lang != null && lang.toLowerCase().startsWith("de");
        List<OrderItemResponse> items = o.getItems().stream().map(oi -> OrderItemResponse.builder()
                .productId(oi.getProduct().getId())
                .productName(de ? oi.getProduct().getNameDe() : oi.getProduct().getNameEn())
                .quantity(oi.getQuantity())
                .price(oi.getPrice())
                .build()
        ).toList();

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
}
