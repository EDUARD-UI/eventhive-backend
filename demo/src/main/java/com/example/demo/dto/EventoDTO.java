package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.example.demo.enums.EstadoEvento;
import com.example.demo.model.Localidad;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventoDTO {
    private Long id;
    private String titulo;
    private String descripcion;
    private String lugar;
    private String foto;
    private LocalDate fecha;
    private LocalTime hora;
    private EstadoEvento estado;
    private EventoCategoriaDTO categoria;
    private EventoOrganizadorDTO organizador;
    private List<Localidad> localidades;
}