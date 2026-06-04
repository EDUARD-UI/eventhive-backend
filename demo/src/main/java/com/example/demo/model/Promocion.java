package com.example.demo.model;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "promociones", indexes = {
    @Index(name = "idx_promo_fecha", columnList = "fecha_inicio, fecha_fin")
})
@Getter @Setter
public class Promocion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    private Double descuento;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "promocion_eventos",
        joinColumns        = @JoinColumn(name = "promocion_id"),
        inverseJoinColumns = @JoinColumn(name = "evento_id"),
        indexes = {
            @Index(name = "idx_pe_evento",    columnList = "evento_id"),
            @Index(name = "idx_pe_promocion", columnList = "promocion_id")
        }
    )
    private List<Evento> eventos;
}