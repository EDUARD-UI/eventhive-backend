package com.eventhive.app.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValoracionRequest {
    @NotNull(message = "La organización es requerida")
    private Long organizacionId;

    @Size(max = 500, message = "El comentario no puede superar los 500 caracteres")
    private String comentario;

    @NotNull(message = "La calificación es obligatoria")
    @Min(value = 1, message = "La calificación debe estar entre 1 y 5")
    @Max(value = 5, message = "La calificación debe estar entre 1 y 5")
    private Long calificacion;
}
