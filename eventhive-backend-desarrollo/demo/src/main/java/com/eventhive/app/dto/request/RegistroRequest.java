package com.eventhive.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistroRequest {

    @NotBlank @Size(max = 150, message = "el nombre no debe superar los 150 caracteres")
    private String nombre;

    @NotBlank
    private String correo;

    @NotBlank @Pattern(regexp = "^[0-9+ ]{7,20}$")
    private String telefono;

    @NotBlank
    private String clave;
}