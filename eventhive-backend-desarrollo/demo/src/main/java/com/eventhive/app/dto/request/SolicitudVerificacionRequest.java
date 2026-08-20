package com.eventhive.app.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SolicitudVerificacionRequest {
    private String razonSocial;
    private String nit;
    private String representanteLegal;
    private String correoEmpresarial;
    private String mensaje;
}