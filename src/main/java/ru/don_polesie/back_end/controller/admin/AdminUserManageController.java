package ru.don_polesie.back_end.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.don_polesie.back_end.dto.user.UserDto;
import ru.don_polesie.back_end.security.SecurityUtils;
import ru.don_polesie.back_end.service.staffOnly.AdminService;


@RestController
@RequiredArgsConstructor
@Tag(
        name = "Администрирование",
        description = "API для управления пользователями и административных функций"
)
@RequestMapping("/api/admin")
@Log4j2
public class AdminUserManageController {

    private final AdminService adminService;
    private final SecurityUtils securityUtils;

    @Operation(
            summary = "Получить список пользователей",
            description = "Возвращает постраничный список всех зарегистрированных пользователей системы"
    )
    @GetMapping("/users")
    public ResponseEntity<Page<UserDto>> findUsersPage(@RequestParam @Min(0) Integer pageNumber) {
        Page<UserDto> usersPage = adminService.findUsersPage(pageNumber);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(usersPage);
    }

    @Operation(
            summary = "Получить список работников",
            description = "Возвращает постраничный список всех сотрудников с правами работника"
    )
    @GetMapping("/workers")
    public ResponseEntity<Page<UserDto>> findWorkersPage(@RequestParam @Min(0) Integer pageNumber) {
        Page<UserDto> workersPage = adminService.findWorkersPage(pageNumber);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(workersPage);
    }

    @Operation(
            summary = "Создать пользователя",
            description = "Создает пользователя с заданными ролями"
    )
    @PostMapping("/user/create")
    public ResponseEntity<Void> createUser(@RequestBody UserDto userDto) {
        adminService.createUser(userDto);

        var user = securityUtils.getCurrentUser();
        log.info("User {} created user {}", user.getPhoneNumber(), userDto.toString());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    @Operation(
            summary = "Обновить пользователя",
            description = "Обновляет пользователя"
    )
    @PostMapping("/user/update")
    public ResponseEntity<Void> updateUser(@RequestBody UserDto userDto) {
        adminService.updateUser(userDto);

        var user = securityUtils.getCurrentUser();

        log.info("User {} updated user {}", user.getPhoneNumber(), userDto.toString());
        return ResponseEntity
                .status(HttpStatus.OK)
                .build();
    }

    @Operation(
            summary = "Деактивировать пользователя",
            description = "Деактивирует пользователя в системе по его идентификатору"
    )
    @DeleteMapping("/user/deactivate/{id}")
    public ResponseEntity<Void> deactivatedUser(@PathVariable @Min(0) Long id) {
        adminService.deactivatedUser(id);

        var user = securityUtils.getCurrentUser();
        log.info("User {} deactivated user {}", user.getPhoneNumber(), id);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @Operation(
            summary = "Активировать пользователя",
            description = "Активирует пользователя в системе по его идентификатору"
    )
    @DeleteMapping("/user/activate/{id}")
    public ResponseEntity<Void> activatedUser(@PathVariable @Min(0) Long id) {
        adminService.activatedUser(id);

        var user = securityUtils.getCurrentUser();
        log.info("User {} activated user {}", user.getPhoneNumber(), id);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}

