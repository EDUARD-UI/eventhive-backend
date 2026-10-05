package com.eventhive.app.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
public class EventoRequest {

    @NotBlank
    @Size(max = 200)
    private String titulo;

    @NotBlank
    private String descripcion;

    @NotBlank @Size(max = 200)
    private String lugar;

    @NotNull
    @FutureOrPresent(message = "La fecha del evento no puede ser en el pasado")
    private LocalDate fecha;

    @NotNull
    private LocalTime hora;

    @NotNull
    private Long categoriaId;

    @NotNull(message = "La latitud es requerida")
    @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90")
    private Double latitud;

    @NotNull(message = "La longitud es requerida")
    @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180")
    private Double longitud;
}