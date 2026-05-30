package ru.don_polesie.back_end.controller.basket.publish;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.don_polesie.back_end.dto.basket.BasketDtoResponse;
import ru.don_polesie.back_end.mapper.BasketMapper;
import ru.don_polesie.back_end.mapper.ProductMapper;
import ru.don_polesie.back_end.security.SecurityUtils;
import ru.don_polesie.back_end.service.basket.BasketService;
import ru.don_polesie.back_end.service.product.ManageProductService;
import ru.don_polesie.back_end.service.product.SearchProductService;
import ru.don_polesie.back_end.service.system.PriceService;

import java.math.BigDecimal;

@RequestMapping("/api/basket")
@AllArgsConstructor
@RestController
public class BasketController {
    private final BasketService basketService;
    private final SecurityUtils securityUtils;
    private final BasketMapper basketMapper;
    private final SearchProductService searchProductService;
    private final ManageProductService manageProductService;
    private final PriceService priceService;
    private final ProductMapper productMapper;

    @GetMapping
    public ResponseEntity<BasketDtoResponse> getBasket() {
        var user = securityUtils.getCurrentUser();
        var resp = basketService.getBasket(user.getPhoneNumber());
        var dto = basketMapper.toDto(resp);
        dto.setTotalPrice(BigDecimal.ZERO);
        dto.getItems().forEach(
                orderItemDto -> {
                    var productRR = searchProductService.findById(orderItemDto.getProductId());
                    var product = productMapper.productDtoRRtoProduct(productRR);
                    orderItemDto.setPrice(product.getPrice());
                    orderItemDto.setProductName(product.getName());
                    orderItemDto.setSale(product.getSale());
                    orderItemDto.setImageUrl(product.getImageUrl());
                    dto.setTotalPrice(dto.getTotalPrice().add(priceService.calculateProductCost(product, orderItemDto.getQuantity())));
                }
        );
        return ResponseEntity.ok(dto);
    }


    @Operation(
            summary = "Добавление товара в корзину",
            description = "Для не весовых товаров по дефолту - 1"
    )
    @GetMapping("/add")
    public ResponseEntity<Void> addProduct(
            @RequestParam @Min(value = 1) Long productId,
            @RequestParam (defaultValue = "1") @Min(value = 1) int quantity) {
        var user = securityUtils.getCurrentUser();
        basketService.addProduct(user.getPhoneNumber(), productId, quantity);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Обновить количество товара в заказу",
            description = "Обновление заказа"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Заказ успешно создан"),
            @ApiResponse(responseCode = "400", description = "Неверные данные заказа"),
            @ApiResponse(responseCode = "404", description = "Товар или адрес не найден")
    })
    @GetMapping("/change-quantity")
    public ResponseEntity<Void> changeProductQuantity(@RequestParam @Min(value = 1) Long productId,
                                                        @RequestParam @Min(value = 1) int quantity) {
        var user = securityUtils.getCurrentUser();
        basketService.changeQuantityProduct(user.getPhoneNumber(), productId, quantity);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Удалить товар из корзины",
            description = "Удаляет конкретный товар из заказа (до момента его обработки)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Товар успешно удален из заказа"),
            @ApiResponse(responseCode = "400", description = "Неверные параметры запроса"),
            @ApiResponse(responseCode = "404", description = "Заказ или товар не найден")
    })
    @DeleteMapping("/product")
    public ResponseEntity<Void> deleteProductFromOrder(@RequestParam @Min(value = 1) Long productId) {
        var user = securityUtils.getCurrentUser();
        basketService.deleteProductFromBasket(user.getPhoneNumber(), productId);
        return ResponseEntity.ok().build();
    }
}
