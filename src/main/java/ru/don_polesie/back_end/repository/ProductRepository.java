package ru.don_polesie.back_end.repository;


import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.don_polesie.back_end.model.product.Brand;
import ru.don_polesie.back_end.model.product.Category;
import ru.don_polesie.back_end.model.product.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrue();

    @NonNull
    Optional<Product> findById(@NonNull Long id);

    void deleteById(@NonNull Long id);

    Page<Product> findAllByAmountGreaterThan(int amount, Pageable pageable);

    Page<Product> findAllBySaleGreaterThan(int i, Pageable pageable);

    @Query("SELECT p FROM Product p " +
            "WHERE (:brand IS NULL OR (LOWER(p.brand.name) LIKE CONCAT('%', LOWER(CAST(:brand AS string)), '%')))" +
            "AND  (:name IS NULL OR (LOWER(p.name) LIKE CONCAT('%', LOWER(CAST(:name AS string)), '%')))" +
            "ORDER BY p.id"
    )
    Page<Product> findProductsByParams(
            @Param("brand") String brand,
            @Param("name") String name,
            Pageable pageable
    );


    // Поиск по названию с устойчивостью к опечаткам (pg_trgm).
    // Совпадает, если:
    //   1) название содержит запрос как подстроку (точный/частичный ввод), ИЛИ
    //   2) триграммное word_similarity запроса к названию > 0.4 — ловит опечатки
    //      («калбаса»→«колбаса»: ~0.50; шум даёт ≤0.38, порог 0.4 их отсекает).
    // Порядок: сначала подстрочные совпадения, затем по убыванию похожести.
    // Native-запрос: word_similarity — функция pg_trgm, в JPQL её нет.
    @Query(value =
            "SELECT * FROM products p WHERE p.active = true AND (" +
            "  lower(p.name) LIKE ('%' || lower(:query) || '%') " +
            "  OR word_similarity(lower(:query), lower(p.name)) > 0.4" +
            ") ORDER BY " +
            "  (lower(p.name) LIKE ('%' || lower(:query) || '%')) DESC, " +
            "  word_similarity(lower(:query), lower(p.name)) DESC, p.id DESC",
            countQuery =
            "SELECT count(*) FROM products p WHERE p.active = true AND (" +
            "  lower(p.name) LIKE ('%' || lower(:query) || '%') " +
            "  OR word_similarity(lower(:query), lower(p.name)) > 0.4)",
            nativeQuery = true)
    Page<Product> searchProductsByQuery(@Param("query") String query, Pageable pageable);

    Optional<Product> findByBrandAndName(Brand brand, String name);

    Page<Product> findPByCategory(@NotNull(message = "Категория обязательна") Category category, Pageable pageable);

    Page<Product> findPByBrand(@NotNull(message = "Бренд обязан быть") Brand brand, Pageable pageable);

    Page<Product> findPByActive(boolean b, Pageable pageable);

    Page<Product> findAllByAmountGreaterThanAndActive(int i, boolean b, Pageable pageable);

    Page<Product> findPByCategoryIdAndActiveTrue(Long category_id, Pageable pageable);

    // Для категории «Новинки»: товары с галочкой isNovelty из 1С плюс те, кому
    // категорию «Новинки» назначили вручную в админке
    @Query("select p from Product p where p.active = true and (p.isNovelty = true or p.category.id = :categoryId)")
    Page<Product> findNoveltyOrCategoryActive(@Param("categoryId") Long categoryId, Pageable pageable);

    Page<Product> findByBrandIdAndActiveTrue(Long id, Pageable pageable);
}
