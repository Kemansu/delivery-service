package ru.don_polesie.back_end.service.auth;

import lombok.AllArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.don_polesie.back_end.dto.auth.response.JwtAuthResponse;
import ru.don_polesie.back_end.exceptions.AccessDeniedException;
import ru.don_polesie.back_end.exceptions.RequestValidationException;
import ru.don_polesie.back_end.model.Role;
import ru.don_polesie.back_end.model.user.User;
import ru.don_polesie.back_end.model.basket.Basket;
import ru.don_polesie.back_end.repository.BasketRepository;
import ru.don_polesie.back_end.repository.RoleRepository;
import ru.don_polesie.back_end.repository.UserRepository;
import ru.don_polesie.back_end.security.admin.JwtTokenProvider;
import ru.don_polesie.back_end.utils.SmsSenderHttpClient;
import ru.don_polesie.back_end.validator.PhoneNumberValidator;

import javax.management.BadAttributeValueExpException;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@AllArgsConstructor
public class UserAuthService {
    private final Random random = new Random();
    private final Map<String, CodeEntry> codes = new ConcurrentHashMap<>();
    private final JWTGeneratorService jwtGeneratorService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BasketRepository basketRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Отправка временного пароля
     */
    public void sendTemporaryPassword(String number) {

        if (userRepository.findByPhoneNumber(number).isPresent()) {
            if (!userRepository.existsByPhoneNumberAndActiveTrue(number)) {
                throw new RequestValidationException("Вы были заблокированы, обратитесь в службу поддержки для решения вопроса");
            }
        }



        if (!PhoneNumberValidator.isValidRussianPhone(number)) {
            throw new RequestValidationException("Некорректный номер телефона");
        }

        String code = generate4LetterCode();
        codes.put(number, new CodeEntry(code, System.currentTimeMillis() + 5 * 60 * 1000));
        SmsSenderHttpClient.sendSms(number, "Ваш код для входа: " + code);
    }

    /**
     * Проверка временного пароля
     */
    public JwtAuthResponse checkTemporaryPassword(String number, String code)  {
        if (!PhoneNumberValidator.isValidRussianPhone(number)) {
            throw new RequestValidationException("Некорректный номер телефона");
        }

        CodeEntry entry = codes.get(number);
        if (entry == null) {
            throw new RequestValidationException("Некорректный номер телефона");
        }

        if (entry.code.equals(code) && System.currentTimeMillis() < entry.expireAt) {
            // Если номера нет в бд - сохраняем нового пользователя
            Optional<User> userOptional = userRepository.findByPhoneNumber(number);
            if (userOptional.isEmpty()) {
                save(number, code);
            } else {
                User user = userOptional.get();
                user.setPassword(passwordEncoder.encode(entry.code));
                userRepository.save(user);
            }
            codes.remove(number);
            return jwtGeneratorService.generateJWT(number, code); // пример, подставь свою реализацию
        }
        throw new BadCredentialsException("Некорректный пароль.");
    }

    /**
     * Обновление access токена по refresh токену
     *
     * @param refreshToken refresh токен
     * @return новый набор access и refresh токенов
     */

    public JwtAuthResponse refresh(String refreshToken) {
        return jwtTokenProvider.refreshUserToken(refreshToken);
    }

    private final PasswordEncoder passwordEncoder;

    /**
     * Регистрация нового пользователя
     */
    @Transactional
    public void save(String phoneNumber, String code) {
        var user = new User();
        user.setPhoneNumber(phoneNumber);
        user.setPassword(passwordEncoder.encode(code));

        // Назначение роли USER по умолчанию
        HashSet<Role> roles = new HashSet<>();
        roles.add(roleRepository.findByName("ROLE_USER").get());
        user.setRoles(roles);

        userRepository.save(user);

        Basket basket = new Basket();
        basket.setUser(user);
        basketRepository.save(basket);
    }

    private String generate4LetterCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            char letter = (char) ('A' + random.nextInt(26));
            code.append(letter);
        }
        return code.toString();
    }

    private static class CodeEntry {
        String code;
        long expireAt;
        CodeEntry(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }
}

