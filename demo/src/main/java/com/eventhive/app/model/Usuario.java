package com.eventhive.app.model;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

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

    @PrePersist
    private void prePersist() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
    }
}