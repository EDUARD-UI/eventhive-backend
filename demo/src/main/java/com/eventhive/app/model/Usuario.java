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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuarios", indexes = {
    @Index(name = "idx_usuario_correo", columnList = "correo", unique = true),
    @Index(name = "idx_usuario_rol",    columnList = "rol_id")
})
@Getter
@Setter
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    // estadísticas de organizadores
    @Column(name = "promedio_rating", nullable = false)
    private Double promedioRating = 0.0;

    @Column(name = "total_valoraciones", nullable = false)
    private Integer totalValoraciones = 0;

    @Column(name = "total_seguidores", nullable = false)
    private Integer totalSeguidores = 0;

    @Column(name = "total_eventos_creados", nullable = false)
    private Integer totalEventosCreados = 0;
}