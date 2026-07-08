package com.eventhive.app.model;

import java.time.LocalDateTime;

import com.eventhive.app.enums.EstadoSolicitud;
import com.eventhive.app.enums.NivelOrganizador;

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

// esta entidad se generara sola por el sistema, si un organizador cumple los requisitos
@Entity
@Table(name = "sugerencias_ascenso", indexes = {
    @Index(name = "idx_sugerencia_organizacion", columnList = "organizacion_id"),
    @Index(name = "idx_sugerencia_estado", columnList = "estado")
})
@Getter
@Setter
public class SugerenciaAscenso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacion_id", nullable = false)
    private Usuario organizacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_actual", nullable = false, length = 20)
    private NivelOrganizador nivelActual;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_sugerido", nullable = false, length = 20)
    private NivelOrganizador nivelSugerido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    @Column(name = "fecha_generacion")
    private LocalDateTime fechaGeneracion;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Usuario administradorQueResolvi;

    @PrePersist
    private void prePersist() {
        if (fechaGeneracion == null) fechaGeneracion = LocalDateTime.now();
    }
}
