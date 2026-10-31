package com.eventhive.app.dto.response;

import java.time.LocalDateTime;

import com.eventhive.app.enums.EstadoInvitacion;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvitacionRolDTO {
    private String correo;
    private String rol;
    private LocalDateTime fechaExpiracion;
    private String organizacionNombre;
    private EstadoInvitacion estado;

    public InvitacionRolDTO(String correo, String rol, LocalDateTime fechaExpiracion) {
        this(correo, rol, fechaExpiracion, null, EstadoInvitacion.PENDIENTE);
    }
}
