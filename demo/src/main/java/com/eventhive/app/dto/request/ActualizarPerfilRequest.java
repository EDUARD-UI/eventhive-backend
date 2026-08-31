package com.eventhive.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActualizarPerfilRequest {

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @Size(max = 20)
    private String telefono;
}