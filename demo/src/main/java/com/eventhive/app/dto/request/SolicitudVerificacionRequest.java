package com.eventhive.app.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SolicitudVerificacionRequest {
    // Datos de la Cuenta de Usuario
    @NotBlank(message = "El nombre es obligatorio")
    private String nombreCompleto;

    @NotBlank(message = "El correo de la cuenta es obligatorio")
    @Email(message = "Correo de cuenta inválido")
    private String correoUsuario;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    // Datos Iniciales de la Organización (Solo Texto)
    @NotBlank(message = "La razón social es obligatoria")
    private String razonSocial;

    @NotBlank(message = "El NIT es obligatorio")
    private String nit;

    @NotBlank(message = "El correo empresarial es obligatorio")
    @Email(message = "Correo empresarial inválido")
    private String correoEmpresarial;
}