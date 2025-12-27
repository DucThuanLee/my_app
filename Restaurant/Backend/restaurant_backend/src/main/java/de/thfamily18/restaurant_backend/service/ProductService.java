package de.thfamily18.restaurant_backend.service;

import de.thfamily18.restaurant_backend.dto.ProductResponse;
import de.thfamily18.restaurant_backend.dto.ProductUpsertRequest;
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

    private final ProductRepository repo;

    public List<ProductResponse> getAll(String lang) {
        return repo.findAll().stream().map(p -> toResponse(p, lang)).toList();
    }

    public List<ProductResponse> getBest(String lang) {
        return repo.findByIsBestSellerTrue().stream().map(p -> toResponse(p, lang)).toList();
    }

    public ProductResponse create(ProductUpsertRequest req) {
        Product p = Product.builder()
                .nameDe(req.nameDe())
                .nameEn(req.nameEn())
                .descriptionDe(req.descriptionDe())
                .descriptionEn(req.descriptionEn())
                .price(req.price())
                .category(req.category())
                .isBestSeller(req.bestSeller())
                .build();
        return toResponse(repo.save(p), "de");
    }

    public ProductResponse update(UUID id, ProductUpsertRequest req) {
        Product p = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        p.setNameDe(req.nameDe());
        p.setNameEn(req.nameEn());
        p.setDescriptionDe(req.descriptionDe());
        p.setDescriptionEn(req.descriptionEn());
        p.setPrice(req.price());
        p.setCategory(req.category());
        p.setBestSeller(req.bestSeller());
        return toResponse(repo.save(p), "de");
    }

    public void delete(UUID id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("Product not found");
        repo.deleteById(id);
    }

    private ProductResponse toResponse(Product p, String lang) {
        boolean de = lang != null && lang.toLowerCase().startsWith("de");
        return ProductResponse.builder()
                .id(p.getId())
                .name(de ? p.getNameDe() : p.getNameEn())
                .description(de ? p.getDescriptionDe() : p.getDescriptionEn())
                .price(p.getPrice())
                .category(p.getCategory())
                .bestSeller(p.isBestSeller())
                .build();
    }
}