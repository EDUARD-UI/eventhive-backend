package com.eventhive.app.dto.response;

import java.time.LocalDateTime;

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
    private String estado;

    public InvitacionRolDTO(String correo, String rol, LocalDateTime fechaExpiracion) {
        this(correo, rol, fechaExpiracion, null, "PENDIENTE");
    }
}
