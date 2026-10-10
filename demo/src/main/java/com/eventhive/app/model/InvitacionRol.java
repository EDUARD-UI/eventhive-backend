package com.eventhive.app.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "invitaciones_rol", indexes = {
        @Index(name = "idx_invitacion_rol_token", columnList = "token_hash", unique = true),
        @Index(name = "idx_invitacion_rol_correo", columnList = "correo_invitado")
})
@Getter
@Setter
public class InvitacionRol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correo_invitado", nullable = false, length = 150)
    private String correoInvitado;

    @Column(name = "rol_destino", nullable = false, length = 50)
    private String rolDestino;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "fecha_invitacion", nullable = false, updatable = false)
    private LocalDateTime fechaInvitacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "fecha_aceptacion")
    private LocalDateTime fechaAceptacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitado_por_id", nullable = false)
    private Usuario invitadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacion_id")
    private Organizacion organizacion;

    @PrePersist
    private void prePersist() {
        if (fechaInvitacion == null) {
            fechaInvitacion = LocalDateTime.now();
        }
    }
}
