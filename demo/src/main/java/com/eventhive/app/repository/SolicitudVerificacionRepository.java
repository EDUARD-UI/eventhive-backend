package com.eventhive.app.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.enums.EstadoSolicitud;
import com.eventhive.app.model.SolicitudVerificacion;

public interface SolicitudVerificacionRepository extends JpaRepository<SolicitudVerificacion, Long> {

    // Obtiene la última solicitud de verificación de un organizador
    @Query("""
        SELECT s FROM SolicitudVerificacion s
        JOIN FETCH s.organizador
        WHERE s.organizador.id = :organizadorId
        ORDER BY s.fechaSolicitud DESC
        """)
    Optional<SolicitudVerificacion> findFirstByOrganizadorId(
            @Param("organizadorId") Long organizadorId);

    // Lista solicitudes de verificación por estado
    @Query("""
        SELECT s FROM SolicitudVerificacion s
        JOIN FETCH s.organizador
        WHERE s.estado = :estado
        ORDER BY s.fechaSolicitud ASC
        """)
    Page<SolicitudVerificacion> findByEstado(
            @Param("estado") EstadoSolicitud estado,
            Pageable pageable);

    // Comprueba si existe una solicitud activa para un organizador
    @Query("""
        SELECT COUNT(s) > 0 FROM SolicitudVerificacion s
        WHERE s.organizador.id = :organizadorId
          AND s.estado = :estado
        """)
    boolean existsByOrganizadorIdAndEstado(
            @Param("organizadorId") Long organizadorId,
            @Param("estado") EstadoSolicitud estado);

    // Comprueba si ya existe una solicitud con el mismo correo empresarial
    @Query("""
        SELECT COUNT(s) > 0 FROM SolicitudVerificacion s
        WHERE s.correoEmpresarial = :correo
          AND s.estado IN ('PENDIENTE', 'APROBADA')
        """)
    boolean existsByCorreoEmpresarial(@Param("correo") String correo);
}
