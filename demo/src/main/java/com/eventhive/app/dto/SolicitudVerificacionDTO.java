package com.eventhive.app.dto;

import java.time.LocalDateTime;

import com.eventhive.app.enums.EstadoSolicitud;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SolicitudVerificacionDTO {

    private Long id;
    private Long organizadorId;
    private String organizadorNombre;
    private String organizadorCorreo;

    // Datos empresariales
    private String razonSocial;
    private String nit;
    private String representanteLegal;
    private String correoEmpresarial;

    // URL pública del RUT en Supabase Storage
    private String urlRut;

    private String mensaje;

    // Enum como tipo fuerte en lugar de String libre
    private EstadoSolicitud estado;

    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaResolucion;
    private String administradorNombre;
    private String motivoRechazo;
}
