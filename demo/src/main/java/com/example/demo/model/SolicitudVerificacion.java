package com.example.demo.model;

import java.time.LocalDateTime;

import com.example.demo.enums.EstadoSolicitud;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "solicitudes_verificacion", indexes = {
    @Index(name = "idx_sv_organizador", columnList = "organizador_id"),
    @Index(name = "idx_sv_estado", columnList = "estado")
})
@Getter
@Setter
public class SolicitudVerificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizador_id", nullable = false)
    private Usuario organizador;

    // Datos de la empresa del organizador
    @Column(name = "razon_social", length = 200)
    private String razonSocial;

    @Column(name = "nit", length = 20)
    private String nit;

    @Column(name = "representante_legal", length = 150)
    private String representanteLegal;

    // Correo con el que el organizador iniciará sesión al ser aprobado
    @Column(name = "correo_empresarial", length = 150)
    private String correoEmpresarial;

    // URL pública del RUT almacenado en Supabase Storage
    @Column(name = "url_rut", length = 500)
    private String urlRut;

    @Column(columnDefinition = "TEXT")
    private String mensaje;

    // Enum persistido como String en base de datos
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    @Column(name = "fecha_solicitud")
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Usuario administradorQueResolvi;

    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
    private String motivoRechazo;
}
