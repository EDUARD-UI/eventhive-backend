package com.eventhive.app.recommendation.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "recomendacion_envios",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_recomendacion_evento_usuario",
            columnNames = {"evento_id", "usuario_id"}
        )
    },
    indexes = {
        @Index(name = "idx_recomendacion_envios_estado", columnList = "estado")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class RecomendacionEnvio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.PENDIENTE;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    public enum Estado {
        PENDIENTE,
        ENVIADO,
        ERROR
    }

    public RecomendacionEnvio(Long eventoId, Long usuarioId, Estado estado) {
        this.eventoId = eventoId;
        this.usuarioId = usuarioId;
        this.estado = estado;
        this.fechaCreacion = LocalDateTime.now();
    }
}

