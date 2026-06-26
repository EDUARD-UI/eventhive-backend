package com.eventhive.app.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValoracionDTO {
    private Long id;
    private String comentario;
    private long calificacion;
    private Long organizadorId;
    private String organizadorNombre;
}
