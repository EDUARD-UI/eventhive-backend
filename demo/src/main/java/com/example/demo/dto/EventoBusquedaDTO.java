package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventoBusquedaDTO {
    private String id;
    private String titulo, nombreCategoria;

    public EventoBusquedaDTO(String id, String titulo, String nombreCategoria) {
        this.id = id;
        this.titulo = titulo;
        this.nombreCategoria = nombreCategoria;
    }
    
}
