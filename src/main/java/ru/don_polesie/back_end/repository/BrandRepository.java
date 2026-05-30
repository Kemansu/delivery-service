package ru.don_polesie.back_end.repository;

import org.springframework.data.repository.CrudRepository;
import ru.don_polesie.back_end.model.product.Brand;
import ru.don_polesie.back_end.model.user.Address;
import ru.don_polesie.back_end.model.user.User;

import java.util.List;
import java.util.Optional;

public interface BrandRepository extends CrudRepository<Brand, Integer> {
    Optional<Brand> findByName(String name);

    List<Brand> findByActiveTrue();

    List<Brand> findByActiveFalse();
}
