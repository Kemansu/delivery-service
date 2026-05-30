package ru.don_polesie.back_end.controller.order.publish;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.don_polesie.back_end.dto.order.response.OrderDtoResponse;
import ru.don_polesie.back_end.security.SecurityUtils;
import ru.don_polesie.back_end.service.order.UserOrderService;


@RestController
@RequiredArgsConstructor
@Tag(
        name = "Заказы пользователя",
        description = "API получения информации о заказах текущего пользователя"
)
@RequestMapping("/api/order")
public class UserInfoOrderController {
    private final UserOrderService orderService;
    private final SecurityUtils securityUtils;

    @Operation(
            summary = "Все заказы",
            description = "Возвращает все заказы пользователя"
    )
    @GetMapping("")
    public ResponseEntity<Page<OrderDtoResponse>> findOrdersPage(@RequestParam @Min(value = 0) Integer pageNumber) {
        String phoneNumber = securityUtils.getCurrentPhoneNumber();
        Page<OrderDtoResponse> ordersPage = orderService.findUserOrdersPage(pageNumber, phoneNumber);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ordersPage);
    }

    @Operation(
            summary = "История доставленных заказов",
            description = "Возвращает историю заказов текущего пользователя с пагинацией"
    )
    @GetMapping("/shipped")
    public ResponseEntity<Page<OrderDtoResponse>> findShippedUserOrdersPage(@RequestParam @Min(value = 0) Integer pageNumber) {
        String phoneNumber = securityUtils.getCurrentPhoneNumber();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(orderService.findShippedUserOrdersPage(pageNumber, phoneNumber));
    }

    @Operation(
            summary = "Получить детали заказа",
            description = "Возвращает полную информацию о конкретном заказе пользователя"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Заказ найден"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден или недоступен")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderDtoResponse> findById(@PathVariable @Min(value = 1) Long id) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(orderService.findById(id));
    }
}
