package com.eventhive.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "promociones", indexes = {
        @Index(name = "idx_promo_evento", columnList = "evento_id"),
        @Index(name = "idx_promo_fecha", columnList = "fecha_inicio, fecha_fin")
})
@Getter
@Setter
public class Promocion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal descuento;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;
}