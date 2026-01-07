package de.thfamily18.restaurant_backend.controller;

import de.thfamily18.restaurant_backend.dto.ProductResponse;
import de.thfamily18.restaurant_backend.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products")
public class ProductController {

    private final ProductService service;

    @Operation(summary = "Get all products")
    @GetMapping
    public List<ProductResponse> getAll(@RequestHeader(name="Accept-Language", defaultValue="de") String lang) {
        return service.getAll(lang);
    }

    @GetMapping("/best")
    public List<ProductResponse> best(@RequestHeader(name="Accept-Language", defaultValue="de") String lang) {
        return service.getBest(lang);
    }
}
