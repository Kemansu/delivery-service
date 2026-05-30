package ru.don_polesie.back_end.controller.order.staff;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.don_polesie.back_end.dto.order.response.OrderDtoResponse;
import ru.don_polesie.back_end.model.enums.OrderStatus;
import ru.don_polesie.back_end.model.user.User;
import ru.don_polesie.back_end.security.SecurityUtils;
import ru.don_polesie.back_end.service.order.WorkerOrderService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/staff/order")
public class OrderInfoController {
    private final WorkerOrderService workOrderService;
    private final SecurityUtils securityUtils;

    @Operation(
            summary = "Заказ по id",
            description = "Возвращает заказ по id"
    )
    @GetMapping("/{id}")
    public ResponseEntity<OrderDtoResponse> findById(@PathVariable @Min(1) Long id) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(workOrderService.findById(id));
    }

    @Operation(
            summary = "Заказ сотрудника для сборки",
            description = "Возвращает заказ по id"
    )
    @GetMapping("/assembly")
    public ResponseEntity<OrderDtoResponse> assembly() {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(workOrderService.findAssemblyOrderForEmployee(user));
    }

    @Operation(
            summary = "Список заказов",
            description = "Возвращает страницу заказов, требующих обработки сотрудниками"
    )
    @GetMapping
    public ResponseEntity<Page<OrderDtoResponse>> findOrdersPage(@RequestParam @Min(0) Integer pageNumber) {
        Page<OrderDtoResponse> ordersPage = workOrderService.findOrdersPage(pageNumber);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ordersPage);
    }

    @Operation(
            summary = "Список заказов по статусу",
            description = "Возвращает страницу заказов с определенным статусом"
    )
    @GetMapping("/status")
    public ResponseEntity<Page<OrderDtoResponse>> findByStatus(@RequestParam @Min(0) Integer pageNumber, @RequestParam String status) {
        OrderStatus orderStatus = OrderStatus.valueOf(status);
        Page<OrderDtoResponse> ordersPage = workOrderService.findOrdersPageWithStatus(pageNumber, orderStatus);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ordersPage);
    }
}
