package ru.don_polesie.back_end.controller.product.admin;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.don_polesie.back_end.dto.product.ProductDtoFull;
import ru.don_polesie.back_end.security.SecurityUtils;
import ru.don_polesie.back_end.service.product.ManageProductService;

@RestController
@RequestMapping("/api/admin/product")
@RequiredArgsConstructor
@Log4j2
public class ProductController {

    private final ManageProductService productServiceImpl;
    private final SecurityUtils securityUtils;

    @Operation(
            summary = "Добавить товар",
            description = "Создание нового товара в каталоге"
    )
    @PostMapping
    public ResponseEntity<ProductDtoFull> save(@RequestBody @Valid ProductDtoFull productDtoFull) {
        ProductDtoFull newProductDtoFull = productServiceImpl.save(productDtoFull);
        var user = securityUtils.getCurrentUser();
        log.info("User {} created product {}", user.getPhoneNumber(), productDtoFull.toString());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(newProductDtoFull);
    }

    @Operation(
            summary = "Обновить товар",
            description = "Обновление информации о существующем товаре"
    )
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@RequestBody @Valid ProductDtoFull productDtoFull,
                                       @PathVariable @Min(value = 1) Long id) {
        productServiceImpl.update(productDtoFull, id);
        var user = securityUtils.getCurrentUser();
        log.info("User {} edit product {}", user.getPhoneNumber(), productDtoFull.toString());
        return ResponseEntity
                .status(HttpStatus.OK)
                .build();
    }

    @Operation(
            summary = "Обновить скидку",
            description = "Обновление скидки товара"
    )
    @PutMapping("sale/{id}")
    public ResponseEntity<Void> editSale(
            @PathVariable @Min(value = 1) Long id,
            @RequestParam @Min(value = 0) @Max(value = 100) Integer sale) {
        productServiceImpl.editSale(id, sale);
        var user = securityUtils.getCurrentUser();
        log.info("User {} edit sale for product {} - {}%", user.getPhoneNumber(), id, sale);
        return ResponseEntity
                .status(HttpStatus.OK)
                .build();
    }

    @Operation(
            summary = "Деактивировать товар",
            description = "Товара больше не будет отображаться в каталоге"
    )
    @PutMapping("deactivate/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable @Min(value = 1) Long id) {
        productServiceImpl.deactivateById(id);
        var user = securityUtils.getCurrentUser();
        log.info("User {} deactivated Product {}", user.getPhoneNumber(), id);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @Operation(
            summary = "Активировать товар",
            description = "Товар будет отображаться в каталоге"
    )
    @PutMapping("activate/{id}")
    public ResponseEntity<Void> activate(@PathVariable @Min(value = 1) Long id) {
        productServiceImpl.activateById(id);
        var user = securityUtils.getCurrentUser();
        log.info("User {} activated Product {}", user.getPhoneNumber(), id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .build();
    }
}
