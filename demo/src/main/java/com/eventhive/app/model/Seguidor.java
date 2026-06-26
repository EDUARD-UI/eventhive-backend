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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(
        name = "seguidores",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_organizador_seguidor", columnNames = {"organizador_id", "seguidor_id"})
        }, indexes = {
            @Index(name = "idx_seguidor_organizador", columnList = "organizador_id"),
            @Index(name = "idx_seguidor_usuario", columnList = "seguidor_id")
        }
)
public class Seguidor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizador_id", nullable = false)
    private Usuario organizador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seguidor_id", nullable = false)
    private Usuario seguidor;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaSeguimiento;

    @PrePersist
    public void prePersist() {
        this.fechaSeguimiento = LocalDateTime.now();
    }
}
