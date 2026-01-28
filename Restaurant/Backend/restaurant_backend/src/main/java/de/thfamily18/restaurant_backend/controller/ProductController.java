package de.thfamily18.restaurant_backend.controller;

import de.thfamily18.restaurant_backend.dto.ProductResponse;
import de.thfamily18.restaurant_backend.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products")
public class ProductController {

    private final ProductService service;

    @Operation(summary = "Get products (optionally filter by category)")
    @GetMapping
    public List<ProductResponse> getProducts(
            @RequestHeader(name = "Accept-Language", defaultValue = "de") String lang,
            @RequestParam(required = false) String category
    ) {
        return service.getProducts(lang, category);
    }

    @Operation(summary = "Get best seller products (optionally filter by category)")
    @GetMapping("/best")
    public List<ProductResponse> getBestSellers(
            @RequestHeader(name = "Accept-Language", defaultValue = "de") String lang,
            @RequestParam(required = false) String category
    ) {
        return service.getBestSellers(lang, category);
    }

}
