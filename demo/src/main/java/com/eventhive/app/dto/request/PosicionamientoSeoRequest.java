package com.eventhive.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PosicionamientoSeoRequest {
    @NotBlank(message = "La URL de la imagen destacada es obligatoria")
    @Size(max = 500, message = "La URL de la imagen destacada no puede superar 500 caracteres")
    private String urlImagenDestacado;
}