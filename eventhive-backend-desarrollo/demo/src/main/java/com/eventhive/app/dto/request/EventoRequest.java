package com.eventhive.app.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
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

    @DecimalMin("-90.0") @DecimalMax("90.0")
    private Double latitud;

    @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double longitud;

    private String fechaPublicacion;
}