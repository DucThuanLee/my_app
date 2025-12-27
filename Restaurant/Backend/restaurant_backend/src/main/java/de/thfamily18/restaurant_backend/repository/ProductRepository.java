package de.thfamily18.restaurant_backend.repository;

import de.thfamily18.restaurant_backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByIsBestSellerTrue();

    List<Product> findByCategoryIgnoreCase(String category);
}
