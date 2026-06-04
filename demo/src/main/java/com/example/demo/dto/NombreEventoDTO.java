package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NombreEventoDTO {
    private String id;
    private String titulo;

    public NombreEventoDTO(String id, String titulo) {
        this.id = id;
        this.titulo = titulo;
    }
    
    public NombreEventoDTO() {}
}
