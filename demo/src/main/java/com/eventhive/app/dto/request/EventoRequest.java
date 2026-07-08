package com.eventhive.app.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventoRequest {
    private String      titulo;
    private String      descripcion;
    private String      lugar;
    private LocalDate   fecha;
    private LocalTime   hora;
    private Long        categoriaId;
    private Double      latitud;
    private Double      longitud;
    private String      fechaPublicacion;
}