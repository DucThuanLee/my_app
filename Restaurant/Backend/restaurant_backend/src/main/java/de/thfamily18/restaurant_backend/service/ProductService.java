package de.thfamily18.restaurant_backend.service;

import de.thfamily18.restaurant_backend.dto.ProductResponse;
import de.thfamily18.restaurant_backend.dto.ProductUpsertRequest;
import de.thfamily18.restaurant_backend.entity.Product;
import de.thfamily18.restaurant_backend.exception.ResourceNotFoundException;
import de.thfamily18.restaurant_backend.repository.ProductRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repo;

    public List<ProductResponse> getProducts(String lang, String category) {
        List<Product> products = (category == null || category.isBlank())
                ? repo.findAll()
                : repo.findByCategoryIgnoreCase(category.trim());

        return products.stream()
                .map(p -> toResponse(p, lang))
                .toList();
    }

    public List<ProductResponse> getBestSellers(String lang, String category) {
        List<Product> products = (category == null || category.isBlank())
                ? repo.findByBestSellerTrue()
                : repo.findByBestSellerTrueAndCategoryIgnoreCase(category.trim());

        return products.stream()
                .map(p -> toResponse(p, lang))
                .toList();
    }


    public ProductResponse create(ProductUpsertRequest req) {
        Product p = mapToEntity(req);
        Product saved = repo.save(p);
        return toResponse(saved, "de"); // admin response default de
    }

    public ProductResponse update(UUID id, ProductUpsertRequest req) {
        Product p = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        applyUpsert(p, req);
        Product saved = repo.save(p);
        return toResponse(saved, "de"); // admin response default de
    }

    public void delete(UUID id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("Product not found");
        repo.deleteById(id);
    }

    @Transactional
    public List<ProductResponse> bulkCreate(@Valid List<@Valid ProductUpsertRequest> reqs) {
        List<Product> products = reqs.stream()
                .map(this::mapToEntity)
                .toList();

        return repo.saveAll(products).stream()
                .map(p -> toResponse(p, "de")) // admin response default de
                .toList();
    }

    // ===== helpers =====

    private String normalizeLang(String langHeader) {
        if (langHeader == null || langHeader.isBlank()) return "de";
        // Accept-Language can be "de-DE,de;q=0.9,en;q=0.8"
        String first = langHeader.split(",")[0].trim();
        String lang = first.split("-")[0].trim().toLowerCase(Locale.ROOT);
        return lang.equals("en") ? "en" : "de";
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

    private Product mapToEntity(ProductUpsertRequest req) {
        Product p = Product.builder().build();
        applyUpsert(p, req);
        return p;
    }

    private void applyUpsert(Product p, ProductUpsertRequest req) {
        p.setNameDe(req.nameDe());
        p.setNameEn(req.nameEn());
        p.setDescriptionDe(req.descriptionDe());
        p.setDescriptionEn(req.descriptionEn());
        p.setPrice(req.price());
        p.setCategory(req.category());
        p.setBestSeller(req.bestSeller());
    }

    public ProductResponse getOne(UUID id, String langHeader) {
        Product p = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        String lang = normalizeLang(langHeader);
        return toResponse(p, lang);
    }
}