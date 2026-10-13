package com.eventhive.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AceptarInvitacionRolRequest {

    @NotBlank
    private String token;

    @Size(max = 150)
    private String nombre;

    @Pattern(regexp = "^[0-9+ ]{7,20}$")
    private String telefono;

    @Size(min = 8, max = 72)
    private String clave;
}
