package com.eventhive.app.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InvitacionRolDTO {
    private String correo;
    private String rol;
    private LocalDateTime fechaExpiracion;
}
