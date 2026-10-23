package com.eventhive.app.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshRequest {

    @NotBlank(message = "El refreshToken es requerido")
    @JsonAlias({"refresh_token", "token"})
    private String refreshToken;
}