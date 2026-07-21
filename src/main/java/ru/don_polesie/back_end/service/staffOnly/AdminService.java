package ru.don_polesie.back_end.service.staffOnly;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.BeanDefinitionDsl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.don_polesie.back_end.dto.user.UserDto;
import ru.don_polesie.back_end.exceptions.ConflictDataException;
import ru.don_polesie.back_end.exceptions.ObjectNotFoundException;
import ru.don_polesie.back_end.model.basket.Basket;
import ru.don_polesie.back_end.model.user.User;
import ru.don_polesie.back_end.model.Role;
import ru.don_polesie.back_end.repository.RoleRepository;
import ru.don_polesie.back_end.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {
    // Константы для пагинации и ролей
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String DEFAULT_SORT_FIELD = "id";
    private static final String WORKER_ROLE_NAME = "ROLE_WORKER";
    private static final String ADMIN_ROLE_NAME = "ROLE_ADMIN";
    private static final String USER_ROLE_NAME = "ROLE_USER";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    /**
     * Получает страницу с пользователями (роль USER)
     *
     * @param pageNumber номер страницы (начинается с 1)
     * @return страница с пользователями в формате DTO
     */

    public Page<UserDto> findUsersPage(Integer pageNumber) {
        Pageable pageable = createDefaultPageable(pageNumber);
        Page<User> usersPage = userRepository.findByRolesName(USER_ROLE_NAME, pageable);
        return usersPage.map(this::toDTO);
    }

    /**
     * Получает страницу с работниками (роль WORKER)
     *
     * @param pageNumber номер страницы (начинается с 1)
     * @return страница с работниками в формате DTO
     */

    public Page<UserDto> findWorkersPage(Integer pageNumber) {
        Pageable pageable = createDefaultPageable(pageNumber);
        Page<User> workersPage =
                userRepository.findDistinctByRolesNameIn(List.of(WORKER_ROLE_NAME, ADMIN_ROLE_NAME), pageable);
        return workersPage.map(this::toDTO);
    }

    /**
     * Удаляет пользователя по идентификатору
     *
     * @param id идентификатор пользователя
     */
    @Transactional
    public void deactivatedUser(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.setActive(false);
            userRepository.save(user);
            return;
        }
        throw new ObjectNotFoundException("User not found");
    }

    @Transactional
    public void activatedUser(@Min(0) Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.setActive(true);
            userRepository.save(user);
            return;
        }
        throw new ObjectNotFoundException("User not found");
    }

    /**
     * Создает нового пользователя
     *
     * @param userDtoResponse данные пользователя для создания
     */
    @Transactional
    public void createUser(UserDto userDtoResponse) {
        checkUniqueness(userDtoResponse, null);
        User user = createUserFromDTO(userDtoResponse);
        if (user.getBasket() == null) {
            Basket basket = new Basket();
            basket.setUser(user);
            user.setBasket(basket);
        }
        userRepository.save(user);
    }

    @Transactional
    public void updateUser(UserDto userDtoResponse) {
        checkUniqueness(userDtoResponse, (long) userDtoResponse.getId());
        User user = createUserFromDTO(userDtoResponse);
        userRepository.save(user);
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Проверяет, что телефон/email/логин не заняты другим пользователем —
     * иначе сохранение падало бы 500-й с SQL-текстом наружу.
     *
     * @param dto       данные пользователя
     * @param excludeId id самого пользователя при обновлении (null при создании)
     */
    private void checkUniqueness(UserDto dto, Long excludeId) {
        if (dto.getPhoneNumber() != null) {
            userRepository.findByPhoneNumber(dto.getPhoneNumber())
                    .filter(u -> !u.getId().equals(excludeId))
                    .ifPresent(u -> {
                        throw new ConflictDataException(
                                "Телефон " + dto.getPhoneNumber() + " уже занят пользователем №" + u.getId());
                    });
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            userRepository.findByEmail(dto.getEmail())
                    .filter(u -> !u.getId().equals(excludeId))
                    .ifPresent(u -> {
                        throw new ConflictDataException(
                                "Email " + dto.getEmail() + " уже занят пользователем №" + u.getId());
                    });
        }
        if (dto.getLogin() != null && !dto.getLogin().isBlank()) {
            userRepository.findByLoginIgnoreCase(dto.getLogin().trim())
                    .filter(u -> !u.getId().equals(excludeId))
                    .ifPresent(u -> {
                        throw new ConflictDataException(
                                "Логин «" + dto.getLogin().trim() + "» уже занят пользователем №" + u.getId());
                    });
        }
    }


    /**
     * Создает объект пагинации с настройками по умолчанию
     *
     * @param pageNumber номер страницы (начинается с 1, преобразуется в 0-based)
     * @return настроенный объект Pageable
     */
    private Pageable createDefaultPageable(Integer pageNumber) {
        int page = pageNumber != null ? Math.max(0, pageNumber - 1) : 0;
        return PageRequest.of(page, DEFAULT_PAGE_SIZE, Sort.by(DEFAULT_SORT_FIELD).descending());
    }

    /**
     * Преобразует сущность User в DTO
     *
     * @param user сущность пользователя
     * @return DTO пользователя
     */
    private UserDto toDTO(User user) {
        UserDto dto = new UserDto();
        dto.id = user.getId().intValue();
        dto.name = user.getName();
        dto.surname = user.getSurname();
        dto.email = user.getEmail();
        dto.phoneNumber = user.getPhoneNumber();
        dto.roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        dto.isActive = user.isActive();
        dto.login = user.getLogin();
        return dto;
    }

    /**
     * Создает сущность User из DTO с шифрованием пароля
     *
     * @param userDtoResponse DTO с данными пользователя
     * @return сущность User с зашифрованным паролем
     */
    private User createUserFromDTO(UserDto userDtoResponse) {
        Optional<User> userOptional = userRepository.findById(Long.valueOf(userDtoResponse.getId()));
        User user = new User();
        if (userOptional.isPresent()) {
            user = userOptional.get();
        }
        if (userDtoResponse.getName() != null) {
            user.setName(userDtoResponse.getName());
        }
        if (userDtoResponse.getSurname() != null) {
            user.setSurname(userDtoResponse.getSurname());
        }
        if (userDtoResponse.getEmail() != null) {
            user.setEmail(userDtoResponse.getEmail());
        }
        if (userDtoResponse.getPhoneNumber() != null) {
            user.setPhoneNumber(userDtoResponse.getPhoneNumber());
        }
        if (userDtoResponse.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(userDtoResponse.getPassword()));
        }
        if (userDtoResponse.getLogin() != null) {
            String login = userDtoResponse.getLogin().trim();
            user.setLogin(login.isEmpty() ? null : login);
        }
        if (userDtoResponse.getRoles() != null) {
            user.setRoles(
                    userDtoResponse.getRoles().stream()
                            .map(roleName -> roleRepository.findByName(roleName)
                                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName)))
                            .collect(Collectors.toSet())
            );
        }
        return user;
    }

}