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
public class InvitacionRolEnviadaDTO {
    private Long id;
    private String correo;
    private String rol;
    private LocalDateTime fechaInvitacion;
    private LocalDateTime fechaEnvio;
    private LocalDateTime fechaExpiracion;
    private LocalDateTime fechaAceptacion;
    private EstadoInvitacion estado;
    private String organizacionNombre;

    public InvitacionRolEnviadaDTO(Long id, String correo, String rol,
                                   LocalDateTime fechaInvitacion, LocalDateTime fechaExpiracion,
                                   LocalDateTime fechaAceptacion, EstadoInvitacion estado, String organizacionNombre) {
        this(id, correo, rol, fechaInvitacion, fechaInvitacion, fechaExpiracion, fechaAceptacion, estado, organizacionNombre);
    }

    public InvitacionRolEnviadaDTO(Long id, String correo, String rol,
                                   LocalDateTime fechaInvitacion, LocalDateTime fechaExpiracion,
                                   LocalDateTime fechaAceptacion) {
        this(id, correo, rol, fechaInvitacion, fechaInvitacion, fechaExpiracion, fechaAceptacion,
                calcularEstado(fechaAceptacion, fechaExpiracion), null);
    }

    public static EstadoInvitacion calcularEstado(LocalDateTime fechaAceptacion, LocalDateTime fechaExpiracion) {
        if (fechaAceptacion != null) {
            return EstadoInvitacion.ACEPTADA;
        }
        if (fechaExpiracion != null && !fechaExpiracion.isAfter(LocalDateTime.now())) {
            return EstadoInvitacion.EXPIRADA;
        }
        return EstadoInvitacion.PENDIENTE;
    }
}
