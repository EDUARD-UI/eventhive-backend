package com.eventhive.app.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventoCategoriaDTO {
    private Long id;
    private String nombre;

    public EventoCategoriaDTO(Long id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }
}