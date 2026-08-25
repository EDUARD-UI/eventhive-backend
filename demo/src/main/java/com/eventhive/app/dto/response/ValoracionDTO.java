package com.eventhive.app.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValoracionDTO {
    private Long   id;
    private String comentario;
    private long   calificacion;
    private Long   organizacionId;
    private String organizacionNombre;
    private Long   clienteId;
    private String clienteNombre;
}