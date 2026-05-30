package ru.don_polesie.back_end.service.product;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.don_polesie.back_end.dto.product.ProductDtoFull;
import ru.don_polesie.back_end.exceptions.ConflictDataException;
import ru.don_polesie.back_end.exceptions.ObjectNotFoundException;
import ru.don_polesie.back_end.model.product.Brand;
import ru.don_polesie.back_end.model.product.Category;
import ru.don_polesie.back_end.model.product.Product;
import ru.don_polesie.back_end.repository.BrandRepository;
import ru.don_polesie.back_end.repository.CategoryRepository;
import ru.don_polesie.back_end.repository.ProductRepository;

import javax.management.BadAttributeValueExpException;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class BrandService {
    private BrandRepository brandRepository;
    private ProductRepository productRepository;

    @Transactional
    public void save(Brand brand) {
        if (brandRepository.findByName(brand.getName()).isPresent()) {
            throw new RuntimeException("Brand " + brand.getName() + " already exists");
        }
        brandRepository.save(brand);
    }

    public List<Brand> findAll() {
        return brandRepository.findByActiveTrue();
    }

    public Brand findByName(String name) {
        return brandRepository.findByName(name).orElse(null);
    }

    @Transactional
    public void update(@Min(1) Integer id, String name) {
        Optional<Brand> brand = brandRepository.findById(id);
        if (brand.isEmpty()) {
            throw new ObjectNotFoundException("Брэнд невозможно удалить: такой категории не существует");
        }
        brand.get().setName(name);
        brandRepository.save(brand.get());
    }

    @Transactional
    public void remove(@Min(1) Integer id) {
        Optional<Brand> brand = brandRepository.findById(id);
        if (brand.isEmpty()) {
            throw new ObjectNotFoundException("Брэнд невозможно удалить: такого брэенда не существует");
        }
        Page<Product> page = productRepository.findPByBrand(brand.get(),  PageRequest.of(0, 1, Sort.by("id").descending()));
        if (!page.isEmpty()) {
            throw new ConflictDataException("Бренд невозможно удалить: существуют товары с таким брэендом.");
        }
        brandRepository.deleteById(id);
    }

    @Transactional
    public void deactivate(@Min(1) Integer id) {
        Optional<Brand> brand = brandRepository.findById(id);
        if (brand.isEmpty()) {
            throw new ObjectNotFoundException("Брэнд невозможно деактивировать: такого брэенда не существует");
        }
        brand.get().setActive(false);
        brandRepository.save(brand.get());
    }

    @Transactional
    public void activate(@Min(1) Integer id) {
        Optional<Brand> brand = brandRepository.findById(id);
        if (brand.isEmpty()) {
            throw new ObjectNotFoundException("Брэнд невозможно активировать: такого брэенда не существует");
        }
        brand.get().setActive(true);
        brandRepository.save(brand.get());
    }

    public List<Brand> findAllDeactivated() {
        return brandRepository.findByActiveFalse();
    }
}
