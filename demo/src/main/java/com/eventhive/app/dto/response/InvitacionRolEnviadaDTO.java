package com.eventhive.app.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InvitacionRolEnviadaDTO {
    private Long id;
    private String correo;
    private String rol;
    private LocalDateTime fechaInvitacion;
    private LocalDateTime fechaExpiracion;
    private LocalDateTime fechaAceptacion;
}
