package ru.don_polesie.back_end.service.product;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.don_polesie.back_end.dto.product.ProductDtoFull;
import ru.don_polesie.back_end.exceptions.ObjectNotFoundException;
import ru.don_polesie.back_end.mapper.ProductMapper;
import ru.don_polesie.back_end.model.product.Brand;
import ru.don_polesie.back_end.model.product.Category;
import ru.don_polesie.back_end.model.product.Product;
import ru.don_polesie.back_end.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Log4j2
public class ManageProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CategoryService categoryService;
    private final BrandService brandService;

    /**
     * Сохраняет новый товар
     *
     * @param productDtoFull DTO с данными нового товара
     * @return сохраненный DTO товара
     */

    @Transactional
    public ProductDtoFull save(ProductDtoFull productDtoFull) {
        Product newProduct = productMapper.productDtoRRtoProduct(productDtoFull);
        checkBrandAndCategory(newProduct);
        Product savedProduct = productRepository.save(newProduct);
        return productMapper.toProductDtoRR(savedProduct);
    }

    @Transactional
    public void editSale(@Min(value = 1) Long id, @Min(value = 1) @Max(value = 100) Integer sale) {
        Product existingProduct = getProductById(id);
        existingProduct.setSale(sale);
        productRepository.save(existingProduct);
    }

    /**
     * Обновляет данные товара
     *
     * @param productDtoFull новые данные товара
     * @param id             идентификатор обновляемого товара
     * @return обновленный DTO товара
     * @throws ObjectNotFoundException если товар не найден
     */

    @Transactional
    public ProductDtoFull update(ProductDtoFull productDtoFull, @Min(value = 0) Long id) {
        Product existingProduct = getProductById(id);
        updateProductFromDto(existingProduct, productDtoFull);
        Product savedProduct = productRepository.save(existingProduct);
        return productMapper.toProductDtoRR(savedProduct);
    }

    /**
     * Удаляет товар по идентификатору
     *
     * @param id идентификатор товара для удаления
     * @throws ObjectNotFoundException если товар не найден
     */

    @Transactional
    public void deleteById(@Min(value = 0) Long id) {
        if (!productRepository.existsById(id)) {
            throw new ObjectNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    @Transactional
    public void deactivateById(@Min(value = 1) Long id) {
        Optional<Product> productOptional = productRepository.findById(id);
        if (productOptional.isEmpty()) {
            throw new ObjectNotFoundException("Product not found with id: " + id);
        }
        Product product = productOptional.get();
        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional
    public void activateById(@Min(value = 1) Long id) {
        Optional<Product> productOptional = productRepository.findById(id);
        if (productOptional.isEmpty()) {
            throw new ObjectNotFoundException("Product not found with id: " + id);
        }
        Product product = productOptional.get();
        product.setActive(true);
        productRepository.save(product);
    }


    // ========== ПРИВАТНЫЕ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private void checkBrandAndCategory(Product product) {
        String brandName = product.getBrand().getName();
        String categoryName = product.getCategory().getName();
        product.setBrand(brandService.findByName(brandName));
        product.setCategory(categoryService.findByName(categoryName));
        if (product.getBrand() == null) {
            throw new ObjectNotFoundException("Brand not found with name: " + brandName);
        } else if (product.getCategory() == null) {
            throw new ObjectNotFoundException("Category not found with name: " + categoryName);
        }
    }


    /**
     * Находит товар по ID или выбрасывает исключение
     *
     * @param id идентификатор товара
     * @return найденный товар
     * @throws ObjectNotFoundException если товар не найден
     */
    private Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Product not found with id: " + id));
    }

    /**
     * Обновляет поля товара из DTO
     *
     * @param product        сущность товара для обновления
     * @param productDtoFull DTO с новыми значениями полей
     */
    private void updateProductFromDto(Product product, ProductDtoFull productDtoFull) {
        // product.setBrand(new Brand(productDtoRR.getBrand()));
        product.setName(productDtoFull.getName());
        product.setPrice(BigDecimal.valueOf(productDtoFull.getPrice()));
        product.setImageUrl(productDtoFull.getImageUrl());
        product.setFatGrams(BigDecimal.valueOf(productDtoFull.getFatGrams()));
        product.setProteinGrams(BigDecimal.valueOf(productDtoFull.getProteinGrams()));
        product.setCarbohydrateGrams(BigDecimal.valueOf(productDtoFull.getCarbohydrateGrams()));
        product.setEnergyKcalPer100g(BigDecimal.valueOf(productDtoFull.getEnergyKcalPer100g()));
        product.setMinWeight(productDtoFull.getMinWeight());
        product.setMaxWeight(productDtoFull.getMaxWeight());
        product.setStorageTemperatureMin(productDtoFull.getStorageTemperatureMin());
        product.setStorageTemperatureMax(productDtoFull.getStorageTemperatureMax());
        product.setCountryOfOrigin(productDtoFull.getCountryOfOrigin());

        product.setAmount(productDtoFull.getAmount());
        product.setDescription(productDtoFull.getDescription());
        product.setSale(productDtoFull.getSale());
        product.setIsWeighted(productDtoFull.getIsWeighted());

        product.setShelfLife(productDtoFull.getShelfLife());

        Category category = categoryService.findByName(productDtoFull.getCategory());
        if (category == null) {
            throw new ObjectNotFoundException("Category not found with name: " + productDtoFull.getCategory());
        }
        product.setCategory(category);

        Brand brand = brandService.findByName(productDtoFull.getBrand());
        if (brand == null) {
            throw new ObjectNotFoundException("Brand not found with name: " + productDtoFull.getBrand());
        }
        product.setBrand(brand);
    }
}