package com.eventhive.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventoMapaDTO {
    private Long id;
    private String titulo;
    private String descripcion;
    private String categoriaNombre;
    private Double latitud;
    private Double longitud;
}