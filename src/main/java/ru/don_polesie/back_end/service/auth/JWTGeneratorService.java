package ru.don_polesie.back_end.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import ru.don_polesie.back_end.dto.auth.response.JwtAuthResponse;
import ru.don_polesie.back_end.security.admin.JwtTokenProvider;
import ru.don_polesie.back_end.service.staffOnly.StaffService;

@Service
@RequiredArgsConstructor
@Slf4j
public class JWTGeneratorService {
    private final AuthenticationManager authenticationManager;
    private final StaffService userServiceImpl;
    private final JwtTokenProvider jwtTokenProvider;


    public JwtAuthResponse generateJWT(String number, String password) {
        var jwtResponse = new JwtAuthResponse();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        number, password)
        );

        // Получение данных пользователя после успешной аутентификации
        var user = userServiceImpl.getByPhoneNumber(number);

        // Формирование ответа с токенами. Пароль в ответ НЕ кладём — он не нужен
        // фронту и не должен гулять по сети/логам.
        jwtResponse.setId(user.getId());
        jwtResponse.setPhoneNumber(number);
        jwtResponse.setAccessToken(jwtTokenProvider.createAccessToken(
                user.getId(), number, user.getRoles())
        );
        jwtResponse.setRefreshToken(jwtTokenProvider.createRefreshToken(
                user.getId(), number)
        );
        log.info("JWT generated for user id {}", user.getId());
        return jwtResponse;
    }
}
