package com.eventhive.app.dto.response;

import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.enums.MotivosRechazos;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// Registro de historia/auditoría de un evento.
// accion + moderador + fecha + evento + resultado => lo que muestra la tabla de historial.
@Getter
@Setter
public class ModeracionEventoDTO {
    private Long id;
    // Acción realizada: APROBADO | CORRECCION_SOLICITADA | RECHAZADO | SUSPENDIDO
    private String accion;
    // Usuario que ejecutó la acción
    private Long moderadorId;
    private String moderadorNombre;
    // Elemento afectado
    private Long eventoId;
    private String eventoNombre;
    // Resultado (estado en el que quedó el evento)
    private EstadoEvento estadoResultante;
    private MotivosRechazos motivo;
    private String observacion;
    private LocalDateTime fecha;
}
