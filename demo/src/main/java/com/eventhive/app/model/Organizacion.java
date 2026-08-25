package com.eventhive.app.model;

import java.time.LocalDateTime;

import com.eventhive.app.enums.NivelOrganizador;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "organizaciones", indexes = {
        @Index(name = "idx_organizacion_nit", columnList = "nit", unique = true)
})
@Getter @Setter
public class Organizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "representante_id", nullable = false, unique = true)
    private Usuario representante;

    @Column(name = "razon_social", nullable = false, length = 200)
    private String razonSocial;

    @Column(name = "nit", nullable = false, length = 20)
    private String nit;

    @Column(name = "correo_empresarial", length = 150)
    private String correoContacto;

    @Column(name = "url_rut", length = 500)
    private String urlRut;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "promedio_rating", nullable = false)
    private Double promedioRating = 0.0;

    @Column(name = "total_valoraciones", nullable = false)
    private Integer totalValoraciones = 0;

    @Column(name = "total_seguidores", nullable = false)
    private Integer totalSeguidores = 0;

    @Column(name = "total_eventos_creados", nullable = false)
    private Integer totalEventosCreados = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel", nullable = false, length = 20)
    private NivelOrganizador nivel = NivelOrganizador.NIVEL_1;

    @Column(name = "eventos_finalizados", nullable = false)
    private Integer eventosFinalizados = 0;

    @Column(name = "eventos_rechazados", nullable = false)
    private Integer eventosRechazados = 0;

    @PrePersist
    private void prePersist() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
        if (nivel == null) nivel = NivelOrganizador.NIVEL_1;
    }
}

