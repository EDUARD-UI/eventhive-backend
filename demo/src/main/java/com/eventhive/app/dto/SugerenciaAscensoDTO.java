package com.eventhive.app.dto;

import java.time.LocalDateTime;

import com.eventhive.app.enums.EstadoSolicitud;
import com.eventhive.app.enums.NivelOrganizador;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SugerenciaAscensoDTO {
    private Long id;
    private Long organizacionId;
    private String organizacionNombre;
    private NivelOrganizador nivelActual;
    private NivelOrganizador nivelSugerido;
    private EstadoSolicitud estado;
    private LocalDateTime fechaGeneracion;
    private LocalDateTime fechaResolucion;
}
