package de.thfamily18.restaurant_backend.service;

import de.thfamily18.restaurant_backend.dto.ProductResponse;
import de.thfamily18.restaurant_backend.entity.Product;
import de.thfamily18.restaurant_backend.exception.ResourceNotFoundException;
import de.thfamily18.restaurant_backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;

    public List<ProductResponse> getAll(String lang) {
        return repository.findAll()
                .stream()
                .map(p -> ProductResponse.builder()
                        .id(p.getId())
                        .name(lang.equals("de") ? p.getNameDe() : p.getNameEn())
                        .description(lang.equals("de") ? p.getDescriptionDe() : p.getDescriptionEn())
                        .price(p.getPrice())
                        .category(p.getCategory())
                        .bestSeller(p.isBestSeller())
                        .build()
                )
                .toList();
    }

    public Product getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }
}