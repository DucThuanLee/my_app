package de.thfamily18.restaurant_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue
    private UUID id;

    @NotBlank
    @Column(nullable = false)
    private String nameDe;

    @NotBlank
    @Column(nullable = false)
    private String nameEn;

    private String descriptionDe;
    private String descriptionEn;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(nullable = false)
    private BigDecimal price;

    @NotBlank
    private String category;

    private boolean isBestSeller;

    private LocalDateTime createdAt = LocalDateTime.now();
}
