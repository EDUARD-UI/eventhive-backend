package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

// DTO para recibir los 5 campos del formulario de solicitud del organizador
@Getter
@Setter
public class SolicitudVerificacionRequest {
    private String razonSocial;
    private String nit;
    private String representanteLegal;
    private String correoEmpresarial;
    private String mensaje;
}