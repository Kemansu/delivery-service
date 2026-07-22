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


    // Поиск по названию с устойчивостью к опечаткам. Товар совпадает, если:
    //   1) название содержит запрос подстрокой (точный/частичный ввод), ИЛИ
    //   2) триграммное word_similarity > 0.4 (ловит перестановки/лишние буквы:
    //      «калбаса»→«колбаса» ~0.50), ИЛИ
    //   3) какое-то слово названия в пределах Левенштейна ≤2 от запроса — ловит
    //      замены гласных, которые триграммы упускают («тварог»→«творог» =1,
    //      «малако»→«молоко» =2). Окно длины слова отсекает совсем чужие слова.
    // Порядок: подстрочные первыми, затем по большему из триграммной/левенштейн-
    // похожести. Native: word_similarity/levenshtein — из pg_trgm/fuzzystrmatch.
    @Query(value =
            "SELECT * FROM products p WHERE p.active = true AND (" +
            "  lower(p.name) LIKE ('%' || lower(:query) || '%') " +
            "  OR word_similarity(lower(:query), lower(p.name)) > 0.4 " +
            "  OR (SELECT min(levenshtein(w, lower(:query))) " +
            "      FROM regexp_split_to_table(lower(p.name), '[^а-яёa-z0-9]+') AS w " +
            "      WHERE length(w) BETWEEN length(:query) - 2 AND length(:query) + 3) <= 2" +
            ") ORDER BY " +
            "  (lower(p.name) LIKE ('%' || lower(:query) || '%')) DESC, " +
            "  GREATEST(word_similarity(lower(:query), lower(p.name)), " +
            "    CASE WHEN (SELECT min(levenshtein(w, lower(:query))) " +
            "               FROM regexp_split_to_table(lower(p.name), '[^а-яёa-z0-9]+') AS w " +
            "               WHERE length(w) BETWEEN length(:query) - 2 AND length(:query) + 3) IS NULL " +
            "      THEN 0 ELSE 1.0 - (SELECT min(levenshtein(w, lower(:query))) " +
            "               FROM regexp_split_to_table(lower(p.name), '[^а-яёa-z0-9]+') AS w " +
            "               WHERE length(w) BETWEEN length(:query) - 2 AND length(:query) + 3)::float " +
            "      / GREATEST(length(:query), 1) END) DESC, p.id DESC",
            countQuery =
            "SELECT count(*) FROM products p WHERE p.active = true AND (" +
            "  lower(p.name) LIKE ('%' || lower(:query) || '%') " +
            "  OR word_similarity(lower(:query), lower(p.name)) > 0.4 " +
            "  OR (SELECT min(levenshtein(w, lower(:query))) " +
            "      FROM regexp_split_to_table(lower(p.name), '[^а-яёa-z0-9]+') AS w " +
            "      WHERE length(w) BETWEEN length(:query) - 2 AND length(:query) + 3) <= 2)",
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
