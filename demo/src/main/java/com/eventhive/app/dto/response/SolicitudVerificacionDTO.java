package com.eventhive.app.dto.response;

import com.eventhive.app.enums.EstadoSolicitud;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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

    // URL pública del RUT en Supabase Storage
    private String urlRut;

    private String mensaje;
    private EstadoSolicitud estado;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaResolucion;
    private String administradorNombre;
    private String motivoRechazo;
}
