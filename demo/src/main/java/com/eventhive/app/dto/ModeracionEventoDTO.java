package com.eventhive.app.dto;

import java.time.LocalDateTime;

import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.enums.MotivosRechazos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModeracionEventoDTO {
    private Long id;
    private Long moderadorId;
    private String moderadorNombre;
    private EstadoEvento estadoResultante;
    private MotivosRechazos motivo;
    private String observacion;
    private LocalDateTime fecha;
}
