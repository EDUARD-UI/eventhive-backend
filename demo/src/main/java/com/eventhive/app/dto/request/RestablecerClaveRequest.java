package com.eventhive.app.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RestablecerClaveRequest {

    @NotBlank(message = "El token es obligatorio")
    private String token;

    // 72 = límite práctico de BCrypt. Alinear el mínimo con la regla de registro.
    @JsonAlias({"nuevaClave", "password", "newPassword"})
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    private String clave;
}