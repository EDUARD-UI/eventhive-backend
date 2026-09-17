package com.eventhive.app.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistroRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "el nombre no debe superar los 150 caracteres")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    private String correo;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9+ ]{7,20}$", message = "El teléfono tiene un formato inválido")
    private String telefono;

    @NotBlank(message = "La contraseña es obligatoria")
    private String clave;
}