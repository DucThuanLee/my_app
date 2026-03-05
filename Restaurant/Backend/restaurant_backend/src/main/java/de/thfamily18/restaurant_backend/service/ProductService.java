package de.thfamily18.restaurant_backend.service;

import de.thfamily18.restaurant_backend.dto.ProductResponse;
import de.thfamily18.restaurant_backend.dto.ProductUpsertRequest;
import de.thfamily18.restaurant_backend.entity.Product;
import de.thfamily18.restaurant_backend.exception.DuplicateResourceException;
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
public class ProductService {

    private final ProductRepository repo;

    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts(String lang, String category) {
        List<Product> products = (category == null || category.isBlank())
                ? repo.findAll()
                : repo.findByCategoryIgnoreCase(category.trim());

        return products.stream()
                .map(p -> toResponse(p, lang))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getBestSellers(String lang, String category) {
        List<Product> products = (category == null || category.isBlank())
                ? repo.findByBestSellerTrue()
                : repo.findByBestSellerTrueAndCategoryIgnoreCase(category.trim());

        return products.stream()
                .map(p -> toResponse(p, lang))
                .toList();
    }


    @Transactional
    public ProductResponse create(ProductUpsertRequest req) {
        // Normalize early to reduce accidental duplicates (" Burger " vs "burger")
        String category = norm(req.category());
        String nameDe = norm(req.nameDe());
        String nameEn = norm(req.nameEn());

        assertNoDuplicate(category, nameDe, nameEn, null);

        Product p = mapToEntity(req);
        // Ensure normalized values are persisted
        p.setCategory(category);
        p.setNameDe(nameDe);
        p.setNameEn(nameEn);

        Product saved = repo.save(p);
        return toResponse(saved, "de"); // admin response default de
    }

    @Transactional
    public ProductResponse update(UUID id, ProductUpsertRequest req) {
        Product p = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        String category = norm(req.category());
        String nameDe = norm(req.nameDe());
        String nameEn = norm(req.nameEn());

        assertNoDuplicate(category, nameDe, nameEn, id);

        applyUpsert(p, req);
        // Ensure normalized values are persisted
        p.setCategory(category);
        p.setNameDe(nameDe);
        p.setNameEn(nameEn);

        Product saved = repo.save(p);
        return toResponse(saved, "de"); // admin response default de
    }

    @Transactional
    public void delete(UUID id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("Product not found");
        repo.deleteById(id);
    }

    @Transactional
    public List<ProductResponse> bulkCreate(@Valid List<@Valid ProductUpsertRequest> reqs) {
        // 1) Detect duplicates inside the request payload (fast fail)
        // Rule: within same category, nameDe or nameEn must be unique (case-insensitive, trimmed)
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (ProductUpsertRequest r : reqs) {
            String cat = norm(r.category());
            String de = norm(r.nameDe());
            String en = norm(r.nameEn());

            String k1 = (cat == null ? "" : cat.toLowerCase()) + "|de|" + (de == null ? "" : de.toLowerCase());
            String k2 = (cat == null ? "" : cat.toLowerCase()) + "|en|" + (en == null ? "" : en.toLowerCase());

            if (!seen.add(k1) || !seen.add(k2)) {
                throw new DuplicateResourceException("Duplicate product in bulk payload (category/name)");
            }
        }

        // 2) Detect duplicates against DB
        for (ProductUpsertRequest r : reqs) {
            assertNoDuplicate(norm(r.category()), norm(r.nameDe()), norm(r.nameEn()), null);
        }

        // 3) Save all
        List<Product> products = reqs.stream()
                .map(r -> {
                    Product p = mapToEntity(r);
                    p.setCategory(norm(r.category()));
                    p.setNameDe(norm(r.nameDe()));
                    p.setNameEn(norm(r.nameEn()));
                    return p;
                })
                .toList();

        return repo.saveAll(products).stream()
                .map(p -> toResponse(p, "de"))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return repo.findDistinctCategories();
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

    // ===== helpers =====

    private void assertNoDuplicate(String category, String nameDe, String nameEn, UUID excludeId) {
        // Defensive: if your DTO already validates @NotBlank, these should not be null
        if (category == null || nameDe == null || nameEn == null) {
            return;
        }
        if (repo.existsDuplicate(category, nameDe, nameEn, excludeId)) {
            throw new DuplicateResourceException("Product name already exists in this category");
        }
    }

    private String norm(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}