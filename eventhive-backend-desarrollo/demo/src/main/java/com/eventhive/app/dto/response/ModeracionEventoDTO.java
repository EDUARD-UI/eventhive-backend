package com.eventhive.app.dto.response;

import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.enums.MotivosRechazos;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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
