package de.thfamily18.restaurant_backend.repository;

import de.thfamily18.restaurant_backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByCategoryIgnoreCase(String category);

    List<Product> findByBestSellerTrue();

    List<Product> findByBestSellerTrueAndCategoryIgnoreCase(String category);

    @Query("select distinct p.category from Product p order by p.category asc")
    List<String> findDistinctCategories();

    @Query("""
        select (count(p) > 0)
        from Product p
        where p.category = :category
          and (:excludeId is null or p.id <> :excludeId)
          and (
            lower(trim(p.nameDe)) = lower(trim(:nameDe))
            or lower(trim(p.nameEn)) = lower(trim(:nameEn))
          )
    """)
    boolean existsDuplicate(String category, String nameDe, String nameEn, UUID excludeId);
}
