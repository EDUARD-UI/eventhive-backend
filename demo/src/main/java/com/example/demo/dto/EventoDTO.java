package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.example.demo.model.Localidad;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventoDTO {
    String id;
    String titulo, descripcion, lugar, foto;
    LocalDate fecha;
    LocalTime hora;
    Categoria categoria;
    Estado estado;
    Organizador organizador;
    List<Localidad> localidades;

    @Getter
    @Setter
    public static class Categoria {
        String id;
        String nombre;

        public Categoria(String id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }
    }

    @Getter
    @Setter
    public static class Estado {
        String id;
        String nombre;

        public Estado(String id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }
    }

    // ← Agrega esta clase interna
    @Getter
    @Setter
    public static class Organizador {
        String id;
        String nombre;
        Boolean esVerificado;

        public Organizador(String id, String nombre, Boolean esVerificado) {
            this.id = id;
            this.nombre = nombre;
            this.esVerificado = esVerificado;
        }
    }

    public EventoDTO() {
    }

    public EventoDTO(String id, String titulo, String descripcion, String lugar, String foto, LocalDate fecha, LocalTime hora, 
                     Categoria categoria, Estado estado, Organizador organizador) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.lugar = lugar;
        this.foto = foto;
        this.fecha = fecha;
        this.hora = hora;
        this.categoria = categoria;
        this.estado = estado;
        this.organizador = organizador;
    }
}