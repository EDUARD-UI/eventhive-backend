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
public class InvitacionRolEnviadaDTO {
    private Long id;
    private String correo;
    private String rol;
    private LocalDateTime fechaInvitacion;
    private LocalDateTime fechaExpiracion;
    private LocalDateTime fechaAceptacion;
    private LocalDateTime fechaRechazo;
    private String estado;
    private String organizacionNombre;

    public InvitacionRolEnviadaDTO(Long id, String correo, String rol,
                                   LocalDateTime fechaInvitacion, LocalDateTime fechaExpiracion,
                                   LocalDateTime fechaAceptacion, String estado, String organizacionNombre) {
        this(id, correo, rol, fechaInvitacion, fechaExpiracion, fechaAceptacion, null, estado, organizacionNombre);
    }

    public InvitacionRolEnviadaDTO(Long id, String correo, String rol,
                                   LocalDateTime fechaInvitacion, LocalDateTime fechaExpiracion,
                                   LocalDateTime fechaAceptacion) {
        this(id, correo, rol, fechaInvitacion, fechaExpiracion, fechaAceptacion, null,
                calcularEstado(fechaAceptacion, null, fechaExpiracion), null);
    }

    public static String calcularEstado(LocalDateTime fechaAceptacion, LocalDateTime fechaExpiracion) {
        return calcularEstado(fechaAceptacion, null, fechaExpiracion);
    }

    public static String calcularEstado(LocalDateTime fechaAceptacion, LocalDateTime fechaRechazo, LocalDateTime fechaExpiracion) {
        if (fechaAceptacion != null) {
            return "ACEPTADA";
        }
        if (fechaRechazo != null) {
            return "RECHAZADA";
        }
        if (fechaExpiracion != null && !fechaExpiracion.isAfter(LocalDateTime.now())) {
            return "EXPIRADA";
        }
        return "PENDIENTE";
    }
}
