package ru.don_polesie.back_end.repository;

import org.springframework.data.repository.CrudRepository;
import ru.don_polesie.back_end.model.product.Brand;
import ru.don_polesie.back_end.model.product.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends CrudRepository<Category, Integer> {
    Optional<Category> findByName(String name);

    List<Category> findByActiveTrue();

    List<Category> findByActiveFalse();

    boolean existsCategoryByName(String name);


}
