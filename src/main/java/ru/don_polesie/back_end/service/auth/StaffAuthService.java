package ru.don_polesie.back_end.service.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.don_polesie.back_end.dto.auth.request.JwtAuthRequest;
import ru.don_polesie.back_end.dto.auth.response.JwtAuthResponse;
import ru.don_polesie.back_end.exceptions.RequestValidationException;
import ru.don_polesie.back_end.exceptions.TooManyRequestsException;
import ru.don_polesie.back_end.repository.UserRepository;
import ru.don_polesie.back_end.security.admin.JwtTokenProvider;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffAuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JWTGeneratorService jwtGeneratorService;
    private final LoginAttemptService loginAttemptService;

    /**
     * Аутентификация пользователя и выдача JWT токенов
     *
     * @param loginRequest запрос с данными для входа (логин и пароль)
     * @return ответ с access и refresh токенами
     */

    public JwtAuthResponse login(JwtAuthRequest loginRequest) {
        String login = loginRequest.getPhoneNumber();

        // Блокировка перебора по аккаунту (в дополнение к лимиту по IP)
        if (loginAttemptService.isBlocked(login)) {
            long mins = (loginAttemptService.secondsUntilUnlock(login) + 59) / 60;
            throw new TooManyRequestsException(
                    "Слишком много попыток входа. Попробуйте через " + mins + " мин.");
        }

        if (!userRepository.existsByPhoneNumberAndActiveTrue(login)) {
            throw new RequestValidationException("Вы были заблокированы, обратитесь в службу поддержки для решения вопроса");
        }

        try {
            JwtAuthResponse response = jwtGeneratorService.generateJWT(login, loginRequest.getPassword());
            loginAttemptService.loginSucceeded(login);
            return response;
        } catch (BadCredentialsException e) {
            loginAttemptService.loginFailed(login);
            throw e;
        }
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
}
