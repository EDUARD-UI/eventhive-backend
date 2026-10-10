package com.eventhive.app.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvitacionRolRequest {

    @NotBlank
    @Email
    private String correo;

    @NotBlank
    @Pattern(regexp = "^(MODERADOR|MARKETING|OPERADOR)$")
    private String rol;
}
