package com.eventhive.app.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponseDTO {
    private String accessToken;
    private String refreshToken;
    private String tipo;
    private String correo;
    private String rol;
}