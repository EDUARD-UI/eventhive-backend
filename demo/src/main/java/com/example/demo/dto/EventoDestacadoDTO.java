package com.example.demo.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventoDestacadoDTO {
    private String id;
    private String titulo, descripcion, lugar, categoriaNombre, foto;
    private LocalDate fecha;

    public EventoDestacadoDTO() {
    }

    public EventoDestacadoDTO(String id, String titulo, String descripcion, String lugar, String categoriaNombre, String foto, LocalDate fecha) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.lugar = lugar;
        this.categoriaNombre = categoriaNombre;
        this.foto = foto;
        this.fecha = fecha;
    }
}
