package com.eventhive.app.model;

import java.time.LocalDateTime;

import com.eventhive.app.enums.SeveridadPalabra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "palabras_prohibidas", indexes = {
        @Index(name = "idx_palabra_prohibida_texto", columnList = "palabra", unique = true)
})
@Getter
@Setter
public class PalabraProhibida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String palabra;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeveridadPalabra severidad;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    private void prePersist() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
        if (severidad == null) severidad = SeveridadPalabra.SOSPECHOSA;
    }
}
