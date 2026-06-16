package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValoracionDTO {

    private String id;
    private String comentario;
    private long calificacion;
    private String organizadorId;
    private String organizadorNombre;
}
