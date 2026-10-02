package com.eventhive.app.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActualizarOrganizacionRequest {

    @Size(max = 200, message = "La razón social no puede superar 200 caracteres")
    private String razonSocial;

    private String descripcion;

    @Email(message = "El correo de contacto no es válido")
    @Size(max = 150, message = "El correo no puede superar 150 caracteres")
    private String correoContacto;
}
