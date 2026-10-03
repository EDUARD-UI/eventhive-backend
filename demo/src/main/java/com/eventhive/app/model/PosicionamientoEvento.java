package com.eventhive.app.model;

import com.eventhive.app.enums.EstadoPosicionamiento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "posicionamientos_evento", indexes = {
        @Index(name = "idx_posicionamiento_evento", columnList = "evento_id"),
        @Index(name = "idx_posicionamiento_organizador", columnList = "organizador_id"),
        @Index(name = "idx_posicionamiento_estado", columnList = "estado")
})
@Getter
@Setter
public class PosicionamientoEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizador_id", nullable = false)
    private Usuario organizador;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "fecha_contratacion", nullable = false)
    private LocalDateTime fechaContratacion;

    @Column(name = "metodo_pago", length = 50)
    private String metodoPago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPosicionamiento estado = EstadoPosicionamiento.PENDIENTE;

    @PrePersist
    private void prePersist() {
        if (fechaContratacion == null) {
            fechaContratacion = LocalDateTime.now();
        }

        if (estado == null) {
            estado = EstadoPosicionamiento.PENDIENTE;
        }
    }
}