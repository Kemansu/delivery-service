package ru.don_polesie.back_end.controller.order.staff;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.don_polesie.back_end.dto.order.request.ProcessQuantitiesDtoRequest;
import ru.don_polesie.back_end.model.user.User;
import ru.don_polesie.back_end.security.SecurityUtils;
import ru.don_polesie.back_end.service.order.WorkerOrderService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/staff/order")
@Log4j2
public class OrderProcessController {

    private final WorkerOrderService workOrderService;
    private final SecurityUtils securityUtils;

    @Operation(
            summary = "Забрать на сборку заказ",
            description = "Сотрудник начинает сборку заказа"
    )
    @PostMapping("/{id}/assembly")
    public ResponseEntity<Void> assembly(@PathVariable @Min(value = 1) Long id) {
        User user = securityUtils.getCurrentUser();
        workOrderService.takeOrder(id, user);

        log.info("{} took order {}", user.getPhoneNumber(), id);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Отменить заказ",
            description = "С заказом что-то не так - сотрудник отменяет его"
    )
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable @Min(value = 1) Long id) {
        User user = securityUtils.getCurrentUser();
        workOrderService.cancelOrder(id, user);
        log.info("{} cancel order {}", user.getPhoneNumber(), id);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Обработать заказ",
            description = "Обновление весов и статуса заказа в процессе сборки"
    )
    @PostMapping("/{id}/process")
    public ResponseEntity<Void> process(@PathVariable @Min(value = 1) Long id,
                                        @RequestBody @Valid ProcessQuantitiesDtoRequest req) throws BadRequestException {
        workOrderService.processOrder(id, req);
        User user = securityUtils.getCurrentUser();
        log.info("{} processed order {}", user.getPhoneNumber(), id);
        return ResponseEntity.ok().build();
    }

//    @Operation(
//            summary = "Удалить заказ",
//            description = "Полное удаление заказа из системы (административная функция)"
//    )
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> delete(@PathVariable @Min(value = 1) Long id) {
//        // workOrderService.deleteOrder(id);
//        return ResponseEntity.ok().build();
//    }
//
//    @Operation(
//            summary = "Удалить заказ",
//            description = "Полное удаление заказа из системы (административная функция)"
//    )
//    @DeleteMapping("/{order_id}/{product_id}")
//    public ResponseEntity<Void> deleteProductFromOrder(@PathVariable @Min(value = 1) Long order_id,
//                                       @PathVariable @Min(value = 1) Long product_id) {
//        workOrderService.deleteProductFromOrder(order_id, product_id);
//        return ResponseEntity.ok().build();
//    }

    @Operation(
            summary = "Отметить как отправленный",
            description = "Изменение статуса заказа на 'отправлен' после комплектации"
    )
    @PutMapping("/ship/{id}")
    public ResponseEntity<Void> markShipped(@PathVariable @Min(value = 1) Long id) {
        workOrderService.markShipped(id);
        User user = securityUtils.getCurrentUser();
        log.info("{} mark shipped order {}", user.getPhoneNumber(), id);
        return ResponseEntity.ok().build();
    }
}
