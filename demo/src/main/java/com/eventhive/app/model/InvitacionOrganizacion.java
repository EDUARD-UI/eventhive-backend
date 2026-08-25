package com.eventhive.app.model;

import com.eventhive.app.enums.EstadoInvitacion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "invitaciones_organizacion", indexes = {
        @Index(name = "idx_invitacion_organizacion", columnList = "organizacion_id"),
        @Index(name = "idx_invitacion_correo", columnList = "correo_invitado"),
        @Index(name = "idx_invitacion_estado", columnList = "estado")
})
@Getter @Setter
public class InvitacionOrganizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacion_id", nullable = false)
    private Organizacion organizacion;

    // Correo con el que se invita; debe existir como CLIENTE
    @Column(name = "correo_invitado", nullable = false, length = 150)
    private String correoInvitado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitado_por_id", nullable = false)
    private Usuario invitadoPor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoInvitacion estado = EstadoInvitacion.PENDIENTE;

    @Column(name = "fecha_invitacion", nullable = false, updatable = false)
    private LocalDateTime fechaInvitacion;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    @PrePersist
    private void prePersist() {
        if (fechaInvitacion == null) fechaInvitacion = LocalDateTime.now();
    }
}