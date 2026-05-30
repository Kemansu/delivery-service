package ru.don_polesie.back_end.service.product;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
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
import ru.don_polesie.back_end.repository.CategoryRepository;
import ru.don_polesie.back_end.repository.ProductRepository;

import javax.management.BadAttributeValueExpException;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class CategoryService {
    private CategoryRepository categoryRepository;
    private ProductRepository productRepository;

    @Transactional
    public void save(Category category) {
        if (categoryRepository.findByName(category.getName()).isPresent()) {
            throw new RuntimeException("Category " + category.getName() + " already exists");
        }
        categoryRepository.save(category);
    }

    public List<Category> findAll() {
        return categoryRepository.findByActiveTrue();
    }

    public Category findByName(String name) {
        return categoryRepository.findByName(name).orElse(null);
    }

    @Transactional
    public void update(@Min(1) Integer id, String name) throws BadAttributeValueExpException {
        Optional<Category> category = categoryRepository.findById(id);
        if (category.isEmpty()) {
            throw new ObjectNotFoundException("Категорию невозможно удалить: такой категории не существует");
        }
        category.get().setName(name);
        categoryRepository.save(category.get());
    }

    @Transactional
    public void remove(@Min(1) Integer id) {
        Optional<Category> category = categoryRepository.findById(id);
        if (category.isEmpty()) {
            throw new ObjectNotFoundException("Категорию невозможно удалить: такой категории не существует");
        }
        Page<Product> page = productRepository.findPByCategory(category.get(), PageRequest.of(0, 1, Sort.by("id").descending()));
        if (!page.isEmpty()) {
            throw new ConflictDataException("Категорию невозможно удалить: существуют товары с такой категорией.");
        }

        categoryRepository.deleteById(id);
    }

    @Transactional
    public void deactivate(@Min(1) Integer id) {
        Optional<Category> category = categoryRepository.findById(id);
        if (category.isEmpty()) {
            throw new ObjectNotFoundException("Категорию невозможно деактивировать: такой категории не существует");
        }
        category.get().setActive(false);
        categoryRepository.save(category.get());
    }

    @Transactional
    public void activate(@Min(1) Integer id) {
        Optional<Category> category = categoryRepository.findById(id);
        if (category.isEmpty()) {
            throw new ObjectNotFoundException("Категорию невозможно активировать: такой категории не существует");
        }
        category.get().setActive(true);
        categoryRepository.save(category.get());
    }

    public List<Category> findAllDeactivated() {
        return categoryRepository.findByActiveFalse();
    }
}
