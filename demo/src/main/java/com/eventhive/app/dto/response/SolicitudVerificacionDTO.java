package com.eventhive.app.dto.response;

import java.time.LocalDateTime;

import com.eventhive.app.enums.EstadoSolicitud;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SolicitudVerificacionDTO {

    private Long id;
    private Long organizacionId;
    private Long representanteId;
    private String representanteNombre;
    private String representanteCorreo;

    // Datos empresariales
    private String razonSocial;
    private String nit;
    private Long representanteLegal;
    private String correoEmpresarial;

    private String mensaje;
    private EstadoSolicitud estado;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaResolucion;
    private String administradorNombre;
    private String motivoRechazo;
}
