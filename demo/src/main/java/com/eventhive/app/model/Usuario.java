package com.eventhive.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios", indexes = {
    @Index(name = "idx_usuario_correo", columnList = "correo", unique = true),
    @Index(name = "idx_usuario_rol",    columnList = "rol_id"),
    @Index(name = "idx_usuario_organizacion", columnList = "organizacion_id")
})
@Getter
@Setter
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Solo se asigna cuando el usuario (rol ORGANIZACION) es APROBADO en su
    // SolicitudVerificacion; antes de eso permanece en null.
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "organizacion_id", unique = true)
    private Organizacion organizacion;

    @Column(nullable = false, length = 100)
    private String nombreCompleto;

    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    @Column(length = 20)
    private String telefono;

    @Column(nullable = false)
    private String clave;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @PrePersist
    private void prePersist() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
    }
}