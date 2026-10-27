package com.eventhive.app.model;

import java.time.LocalDateTime;

import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.enums.MotivosRechazos;

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
@Table(name = "moderaciones_evento", indexes = {
    @Index(name = "idx_moderacion_evento", columnList = "evento_id"),
    @Index(name = "idx_moderacion_evento_fecha", columnList = "evento_id, fecha"),
    @Index(name = "idx_moderacion_moderador", columnList = "moderador_id"),
    @Index(name = "idx_moderacion_mod_estado", columnList = "moderador_id, estado_resultante")
})
@Getter
@Setter
public class ModeracionEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderador_id", nullable = true)
    private Usuario moderador;

    // Estado que queda el evento después de la acción de moderación.
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_resultante", nullable = false, length = 20)
    private EstadoEvento estadoResultante;

    // Nulo cuando la acción fue aprobar
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private MotivosRechazos motivo;

    // Solo se diligencia cuando motivo = OTRO
    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @PrePersist
    private void prePersist() {
        if (fecha == null) fecha = LocalDateTime.now();
    }
}
