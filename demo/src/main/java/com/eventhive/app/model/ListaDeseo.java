package com.eventhive.app.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(
    name = "lista_deseos",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_usuario_evento_deseo", columnNames = {"usuario_id", "evento_id"})
    },
    indexes = {
        @Index(name = "idx_deseo_usuario", columnList = "usuario_id"),
        @Index(name = "idx_deseo_evento", columnList = "evento_id")
    }
)
public class ListaDeseo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @Column(name = "fecha_agregado", nullable = false, updatable = false)
    private LocalDateTime fechaAgregado;

    @PrePersist
    public void prePersist() {
        this.fechaAgregado = LocalDateTime.now();
    }
}
