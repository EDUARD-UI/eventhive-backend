package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventoCategoriaDTO {
    private String id;
    private String nombre;

    public EventoCategoriaDTO(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }
}