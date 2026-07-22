package ru.don_polesie.back_end.dto.auth.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JwtAuthResponse {
    private Long id;
    private String phoneNumber;
    private String accessToken;
    private String refreshToken;
}
