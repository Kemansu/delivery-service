package ru.don_polesie.back_end.controller.order.publish;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.don_polesie.back_end.dto.order.response.OrderCreatedDtoResponse;
import ru.don_polesie.back_end.model.user.User;
import ru.don_polesie.back_end.security.SecurityUtils;
import ru.don_polesie.back_end.service.order.UserOrderService;


@RestController
@RequiredArgsConstructor
@Tag(
        name = "Заказы пользователя",
        description = "API для управления заказами текущего пользователя"
)
@RequestMapping("/api/order")
@Log4j2
public class UserManipulationOrderController {
    private final UserOrderService orderServiceImpl;
    private final SecurityUtils securityUtils;

    @Operation(
            summary = "Создать заказ",
            description = "Создание нового заказа на основе выбранных товаров и адреса доставки"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Заказ успешно создан"),
            @ApiResponse(responseCode = "400", description = "Неверные данные заказа"),
            @ApiResponse(responseCode = "404", description = "Товар или адрес не найден")
    })
    @PostMapping
    public ResponseEntity<OrderCreatedDtoResponse> save(@RequestParam @Min(value = 1) Long addressId, @RequestParam(defaultValue = "false") Boolean needCallForApproval) {
        User user = securityUtils.getCurrentUser();
        var resp = orderServiceImpl.save(user, addressId, needCallForApproval);
        log.info("{} created order: {}", user.getPhoneNumber(), resp.toString());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resp);
    }
}
