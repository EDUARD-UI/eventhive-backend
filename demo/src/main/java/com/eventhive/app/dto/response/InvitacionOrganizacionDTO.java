package com.eventhive.app.dto.response;

import com.eventhive.app.enums.EstadoInvitacion;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class InvitacionOrganizacionDTO {
    private Long id;
    private String correoInvitado;
    private String organizacionNombre;
    private String invitadoPorNombre;
    private EstadoInvitacion estado;
    private LocalDateTime fechaInvitacion;
    private LocalDateTime fechaRespuesta;
}