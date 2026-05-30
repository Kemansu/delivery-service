package ru.don_polesie.back_end.service.product;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.ILoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.don_polesie.back_end.dto.order.response.PopularProductDtoResponse;
import ru.don_polesie.back_end.dto.product.ProductDtoFull;
import ru.don_polesie.back_end.dto.product.request.ProductDtoSearchRequest;
import ru.don_polesie.back_end.exceptions.ObjectNotFoundException;
import ru.don_polesie.back_end.mapper.ProductMapper;
import ru.don_polesie.back_end.model.product.Brand;
import ru.don_polesie.back_end.model.product.Category;
import ru.don_polesie.back_end.model.product.Product;
import ru.don_polesie.back_end.repository.OrderProductRepository;
import ru.don_polesie.back_end.repository.ProductRepository;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SearchProductService {
    @Value("${utils.page-size}")
    private int PAGE_SIZE;

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final OrderProductRepository orderProductRepository;


    /**
     * Получает страницу с товарами, имеющими ненулевое количество
     *
     * @param pageNumber номер страницы (начинается с 1)
     * @return страница с товарами в формате DTO
     */
    public Page<ProductDtoFull> findProductsActivatedPage(@Min(value = 0) Integer pageNumber) {
        Pageable pageable = createDefaultPageable(pageNumber);
        // только те товары, которые есть в наличии
        return productRepository.findAllByAmountGreaterThanAndActive(0, true, pageable)
                .map(productMapper::toProductDtoRR);
    }

    public Page<ProductDtoFull> findProductsDeactivatedPage(@Min(value = 0) Integer pageNumber) {
        Pageable pageable = createDefaultPageable(pageNumber);
        // только те товары, которые есть в наличии
        return productRepository.findPByActive(false, pageable)
                .map(productMapper::toProductDtoRR);
    }

    public Page<ProductDtoFull> findProductsPageWithSale(@Min(value = 0) Integer pageNumber) {
        Pageable pageable = createDefaultPageable(pageNumber);
        // только те товары, которые со скидкой
        return productRepository.findAllBySaleGreaterThan(0, pageable)
                .map(productMapper::toProductDtoRR);
    }

    public Page<ProductDtoFull> findProductsByCategory(@Min(value = 0) Integer pageNumber, Long id) {
        Pageable pageable = createDefaultPageable(pageNumber);
        Page<Product> productsPage = productRepository.findPByCategoryIdAndActiveTrue(id, pageable);
        if (!productsPage.hasContent()) {
            throw new ObjectNotFoundException("Продуктов с такой категорией не найдено");
        }
        return productsPage.map(productMapper::toProductDtoRR);
    }

    public Page<ProductDtoFull> findProductsByBrand(@Min(value = 0) Integer pageNumber, Long id) {
        Pageable pageable = createDefaultPageable(pageNumber);
        Page<Product> productsPage = productRepository.findByBrandIdAndActiveTrue(id, pageable);
        if (!productsPage.hasContent()) {
            throw new ObjectNotFoundException("Продуктов с таким брендом не найдено");
        }
        return productsPage.map(productMapper::toProductDtoRR);
    }

    /**
     * Находит товар по идентификатору
     *
     * @param id идентификатор товара
     * @return DTO товара
     * @throws ObjectNotFoundException если товар не найден
     */
    public ProductDtoFull findById(@Min(value = 0) Long id) {
        return productRepository.findById(id)
                .map(productMapper::toProductDtoRR)
                .orElseThrow(() -> new ObjectNotFoundException("Product not found with id: " + id));
    }

    /**
     * Находит товары по параметрам поиска
     *
     * @param productDtoSearch DTO с параметрами поиска (id, бренд, название)
     * @param pageNumber       номер страницы
     * @return страница с найденными товарами
     */
    public Page<ProductDtoFull> findAllByParams(ProductDtoSearchRequest productDtoSearch, @Min(value = 0) Integer pageNumber) {
        Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE);
        return productRepository.findProductsByParams(
                        productDtoSearch.getBrand(),
                        productDtoSearch.getName(),
                        pageable
                )
                .map(productMapper::toProductDtoRR);
    }

    /**
     * Находит товары по текстовому запросу
     *
     * @param query      текстовый запрос для поиска
     * @param pageNumber номер страницы
     * @return страница с найденными товарами
     */
    public Page<ProductDtoFull> findProductByQuery(String query, @Min(value = 0) Integer pageNumber) {
        Pageable pageable = createDefaultPageable(pageNumber);
        return productRepository.searchProductsByQuery(query, pageable)
                .map(productMapper::toProductDtoRR);
    }


    /**
     * Создает объект пагинации с настройками по умолчанию
     *
     * @param pageNumber номер страницы (начинается с 1, преобразуется в 0-based)
     * @return настроенный объект Pageable
     */
    private Pageable createDefaultPageable(Integer pageNumber) {
        return PageRequest.of(pageNumber, PAGE_SIZE, Sort.by("id").descending());
    }

    private ZoneId zone = ZoneId.of("Europe/Moscow");
    public Page<ProductDtoFull> getMostPopularProducts(
            @Min(2025) int year,
            @Min(1) @Max(12) int month,
            @Min(0) int pageNumber) {

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay(zone).toLocalDateTime();
        LocalDateTime endOfMonth = yearMonth.atEndOfMonth().plusDays(1).atStartOfDay(zone).toLocalDateTime();

        Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE);

        // Получаем Page<PopularProductDtoResponse>
        Page<PopularProductDtoResponse> popularProductsPage = orderProductRepository
                .findPopularProductsByPeriod(startOfMonth, endOfMonth, pageable);

        // Получаем список ID продуктов
        List<Long> productIds = popularProductsPage.getContent()
                .stream()
                .map(PopularProductDtoResponse::getProductId)
                .collect(Collectors.toList());

        if (productIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // Загружаем все продукты одним запросом
        Map<Long, Product> productsMap = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // Преобразуем в DTO, сохраняя порядок из популярных продуктов
        List<ProductDtoFull> content = popularProductsPage.getContent()
                .stream()
                .map(popularProduct -> productsMap.get(popularProduct.getProductId()))
                .filter(Objects::nonNull)
                .map(productMapper::toProductDtoRR)
                .collect(Collectors.toList());

        // Создаем новый Page с контентом и метаданными
        return new PageImpl<>(
                content,
                pageable,
                popularProductsPage.getTotalElements()
        );
    }
}
