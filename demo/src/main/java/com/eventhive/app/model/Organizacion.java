package com.eventhive.app.model;

import java.time.LocalDateTime;

import com.eventhive.app.enums.NivelOrganizador;

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

// Perfil legal + estadísticas de una organización ya APROBADA (se crea únicamente
// al aprobar una SolicitudVerificacion, ver ServiceSolicitudVerificacion.aprobarSolicitud)
@Entity
@Table(name = "organizaciones", indexes = {
    @Index(name = "idx_organizacion_nit", columnList = "nit", unique = true)
})
@Getter @Setter
public class Organizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Datos legales de la organización
    @Column(name = "razon_social", nullable = false, length = 200)
    private String razonSocial;

    // NIT o CC del organizador/organización
    @Column(name = "nit", nullable = false, length = 20)
    private String nit;

    @Column(name = "representante_legal", length = 150)
    private String representanteLegal;

    // URL pública del RUT almacenado en Supabase Storage
    @Column(name = "url_rut", length = 500)
    private String urlRut;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    // estadísticas de organizacion
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

