package com.eventhive.app.recommendation.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventoRecomendacionDTO {
    private Long id;
    private String titulo;
    private String descripcion;
    private Long categoriaId;
    private String categoriaNombre;
    private String lugar;
    private LocalDate fecha;
}

