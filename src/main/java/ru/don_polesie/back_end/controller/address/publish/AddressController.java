package ru.don_polesie.back_end.controller.address.publish;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.don_polesie.back_end.dto.address.request.AddressDtoRequest;
import ru.don_polesie.back_end.dto.address.response.AddressDtoResponse;
import ru.don_polesie.back_end.model.user.User;
import ru.don_polesie.back_end.security.SecurityUtils;
import ru.don_polesie.back_end.service.userOnly.UserAddressService;

import java.util.List;


@RestController
@RequiredArgsConstructor
@Tag(
        name = "Адреса доставки",
        description = "API для управления адресами доставки пользователя"
)
@RequestMapping("/api/address")
@Log4j2
public class AddressController {
    private final UserAddressService userAddressService;
    private final SecurityUtils securityUtils;

    @Operation(
            summary = "Получить адреса пользователя",
            description = "Возвращает список всех адресов доставки текущего пользователя"
    )
    @GetMapping
    public ResponseEntity<List<AddressDtoResponse>> findAll() {
        var user = securityUtils.getCurrentUser();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(userAddressService.getUserAddresses(user));
    }

    @Operation(
            summary = "Добавить адрес доставки",
            description = "Создает новый адрес доставки для текущего пользователя"
    )
    @PostMapping()
    public ResponseEntity<String> createUserAddress(@RequestBody AddressDtoRequest address) {
        var user = securityUtils.getCurrentUser();
        log.info("User {} created Address {}", user.getPhoneNumber(), address.toString());
        userAddressService.save(address, user);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    @Operation(
            summary = "Обновить комментарий к адресу",
            description = "Обновляет комментарий к адресу доставки"
    )
    @PostMapping("/comment")
    public ResponseEntity<String> updateCommentForAddress(@RequestParam Long id, @RequestParam String comment) {
        var user = securityUtils.getCurrentUser();
        userAddressService.updateComment(id, comment, user);
        log.info("User {} updated comment {}", user.getPhoneNumber(), comment);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    @Operation(
            summary = "Делает адрес доставки неактивным",
            description = "Адрес доставки по идентификатору больше не будет виден пользователю"
    )
    @PutMapping("deactivate/{id}")
    public ResponseEntity<Void> deactivateUserAddress(@PathVariable @Min(0) Long id) {
        var user = securityUtils.getCurrentUser();
        userAddressService.deactivate(id, user);
        log.info("User {} deactivated Address {}", user.getPhoneNumber(), id);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
