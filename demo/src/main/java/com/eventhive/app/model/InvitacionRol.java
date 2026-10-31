package com.eventhive.app.model;

import java.time.LocalDateTime;

import com.eventhive.app.enums.EstadoInvitacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        @Index(name = "idx_invitacion_rol_correo", columnList = "correo_invitado"),
        @Index(name = "idx_invitacion_rol_org", columnList = "organizacion_id, fecha_invitacion"),
        @Index(name = "idx_invitacion_rol_invitado_por", columnList = "invitado_por_id"),
        @Index(name = "idx_invitacion_rol_estado", columnList = "estado")
})
@Getter
@Setter
public class InvitacionRol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correo_invitado", nullable = false, length = 150)
    private String correoInvitado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_destino_id", nullable = false)
    private Rol rolDestino;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "fecha_invitacion", nullable = false)
    private LocalDateTime fechaInvitacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "fecha_aceptacion")
    private LocalDateTime fechaAceptacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoInvitacion estado = EstadoInvitacion.PENDIENTE;

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
        if (estado == null) {
            estado = EstadoInvitacion.PENDIENTE;
        }
    }

    public EstadoInvitacion getEstado() {
        if (estado == EstadoInvitacion.PENDIENTE && fechaExpiracion != null && !fechaExpiracion.isAfter(LocalDateTime.now())) {
            return EstadoInvitacion.EXPIRADA;
        }
        return estado;
    }

    public boolean isExpirada() {
        return getEstado() == EstadoInvitacion.EXPIRADA;
    }

    public String getRolNombre() {
        return rolDestino != null ? rolDestino.getNombre() : null;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaInvitacion;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaInvitacion = fechaEnvio;
    }
}
