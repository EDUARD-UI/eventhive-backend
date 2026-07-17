package com.eventhive.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshRequest {

    @NotBlank(message = "El refreshToken es requerido")
    private String refreshToken;
}