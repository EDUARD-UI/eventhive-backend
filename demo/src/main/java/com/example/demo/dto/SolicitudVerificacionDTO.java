package com.example.demo.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SolicitudVerificacionDTO {

    private String id;

    private String organizadorId;

    private String organizadorNombre;

    private String organizadorCorreo;

    private String mensaje;

    private String archivoConfirmacion;

    private String estado; // PENDIENTE, APROBADA, RECHAZADA

    private LocalDateTime fechaSolicitud;

    private LocalDateTime fechaResolucion;

    private String administradorNombre;

    private String motivoRechazo;
}
