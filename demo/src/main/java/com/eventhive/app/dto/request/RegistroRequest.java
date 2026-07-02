package com.eventhive.app.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistroRequest {
    private String nombre;
    private String correo;
    private String telefono;
    private String clave;
}