package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventoDetalleDTO {

    private String id;
    private String titulo;
    private String descripcion;
    private String foto;
    private LocalDate fecha;
    private LocalTime hora;
    private String lugar;
    private String categoriaNombre;
    private String estadoNombre;
    private List<LocalidadInfo> localidades;

    @Getter @Setter
    public static class LocalidadInfo {
        private String id;
        private String nombre;
        private BigDecimal precio;
        private Integer capacidad;
        private Integer disponibles;
    }
}