package de.thfamily18.restaurant_backend.controller;

import de.thfamily18.restaurant_backend.dto.ProductResponse;
import de.thfamily18.restaurant_backend.dto.ProductUpsertRequest;
import de.thfamily18.restaurant_backend.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Products")
public class AdminProductController {

    private final ProductService service;

    @PostMapping
    @Operation(summary = "Create product")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden (not ADMIN)")
    })
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductUpsertRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductUpsertRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk create products")
    public ResponseEntity<List<ProductResponse>> bulkCreate(
            @Valid @RequestBody List<@Valid ProductUpsertRequest> reqs
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.bulkCreate(reqs));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ProductResponse getOne(
            @PathVariable UUID id,
            @RequestHeader(name = "Accept-Language", defaultValue = "de") String lang
    ) {
        return service.getOne(id, lang);
    }

    @GetMapping
    @Operation(summary = "List products (optionally filter by category)")
    public List<ProductResponse> list(
            @RequestHeader(name = "Accept-Language", defaultValue = "de") String lang,
            @RequestParam(required = false) String category
    ) {
        return service.getProducts(lang, category);
    }

    @GetMapping("/best")
    @Operation(summary = "List best seller products (optionally filter by category)")
    public List<ProductResponse> best(
            @RequestHeader(name = "Accept-Language", defaultValue = "de") String lang,
            @RequestParam(required = false) String category
    ) {
        return service.getBestSellers(lang, category);
    }
}
