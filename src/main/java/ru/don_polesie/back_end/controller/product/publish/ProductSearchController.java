package ru.don_polesie.back_end.controller.product.publish;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.don_polesie.back_end.dto.SingleStringArg;
import ru.don_polesie.back_end.dto.order.response.PopularProductDtoResponse;
import ru.don_polesie.back_end.dto.product.ProductDtoFull;
import ru.don_polesie.back_end.dto.product.request.ProductDtoSearchRequest;
import ru.don_polesie.back_end.model.product.Brand;
import ru.don_polesie.back_end.model.product.Category;
import ru.don_polesie.back_end.service.product.SearchProductService;

import java.util.List;


@Tag(
        name = "Каталог товаров",
        description = "Публичный API для поиска и просмотра товаров"
)
@RequestMapping("/api/find/product/")
@RestController
@RequiredArgsConstructor
public class ProductSearchController {

    private final SearchProductService searchProductService;

    @Operation(
            summary = "Получить активные товары с пагинацией",
            description = "Возвращает страницу с товарами, отсортированными по идентификатору"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список товаров успешно получен")
    })
    @GetMapping
    public ResponseEntity<Page<ProductDtoFull>> findProductsPage(@RequestParam @Min(value = 0) Integer pageNumber) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(searchProductService.findProductsActivatedPage(pageNumber));
    }

    @Operation(
            summary = "Получить неактивные товары с пагинацией",
            description = "Возвращает страницу с товарами, отсортированными по идентификатору"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список товаров успешно получен")
    })
    @RolesAllowed({"ADMIN", "WORKER"})
    @GetMapping("/deactivated")
    public ResponseEntity<Page<ProductDtoFull>> findDeactivatedProductsPage(@RequestParam @Min(value = 0) Integer pageNumber) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(searchProductService.findProductsDeactivatedPage(pageNumber));
    }

    @Operation(
            summary = "Получить товары с пагинацией со скидкой",
            description = "Возвращает страницу с товарами со скидкой"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список товаров успешно получен")
    })
    @GetMapping("/sale")
    public ResponseEntity<Page<ProductDtoFull>> findProductsPageWithSale(@RequestParam @Min(value = 0) Integer pageNumber) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(searchProductService.findProductsPageWithSale(pageNumber));
    }

    @Operation(
            summary = "Найти товар по ID",
            description = "Возвращает полную информацию о товаре по его идентификатору"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Товар найден"),
            @ApiResponse(responseCode = "404", description = "Товар не найден")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductDtoFull> findById(@PathVariable @Min(value = 1) Long id) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(searchProductService.findById(id));
    }

    @Operation(
            summary = "Расширенный поиск товаров",
            description = "Поиск товаров по различным фильтрам с пагинацией. Все параметры необязательные."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Поиск выполнен успешно"),
            @ApiResponse(responseCode = "400", description = "Неверные параметры фильтрации")
    })
    @GetMapping("/by-params")
    public ResponseEntity<Page<ProductDtoFull>> findAllByParams(@RequestParam(required = false) String brand,
                                                                @RequestParam(required = false) String name,
                                                                @RequestParam(required = false) Integer pageNumber) {
        ProductDtoSearchRequest productDtoSearch = ProductDtoSearchRequest
                .builder()
                .brand(brand)
                .name(name)
                .build();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(searchProductService.findAllByParams(productDtoSearch, pageNumber));
    }

    @Operation(
            summary = "Поиск товаров по текстовому запросу",
            description = "Полнотекстовый поиск товаров по названию, бренду и другим текстовым полям"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Поиск выполнен успешно"),
            @ApiResponse(responseCode = "400", description = "Пустой или неверный поисковый запрос")
    })
    @PostMapping("/by-query")
    public ResponseEntity<Page<ProductDtoFull>> findProductsByQuery(@RequestParam String query,
                                                                    @RequestParam Integer pageNumber) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(searchProductService.findProductByQuery(query, pageNumber));
    }

    @Operation(
            summary = "Товары по популярности"
    )
    @GetMapping("/most-popular-products")
    public ResponseEntity<Page<ProductDtoFull>> mostPopularProducts(@RequestParam @Min(2025) int year,
                                                                               @RequestParam @Min(1) @Max(12) int month,
                                                                               @RequestParam int pageNumber) {
        Page<ProductDtoFull> products = searchProductService.getMostPopularProducts(year, month, pageNumber);
        if (products.isEmpty()) {
            products = searchProductService.findProductsActivatedPage(pageNumber);
        }
        return ResponseEntity.ok(products);
    }

    @GetMapping("/by-category")
    public ResponseEntity<Page<ProductDtoFull>> findProductByCategory(@RequestParam Long id,
                                                                      @RequestParam Integer pageNumber){
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(searchProductService.findProductsByCategory(pageNumber, id));
    }

    @GetMapping("/by-brand")
    public ResponseEntity<Page<ProductDtoFull>> findProductByBrand(@RequestParam Long id,
                                                                      @RequestParam Integer pageNumber){
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(searchProductService.findProductsByBrand(pageNumber, id));
    }
}
