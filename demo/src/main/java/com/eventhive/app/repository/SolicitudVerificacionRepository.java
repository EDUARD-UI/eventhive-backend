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

    // Obtiene la última solicitud de verificación de un representante
    @Query("""
        SELECT s FROM SolicitudVerificacion s
        JOIN FETCH s.representanteLegal
        WHERE s.representanteLegal.id = :representanteId
        ORDER BY s.fechaSolicitud DESC
        """)
    Optional<SolicitudVerificacion> findFirstByRepresentanteId(@Param("representanteId") Long representanteId);

    @Query("""
        SELECT s FROM SolicitudVerificacion s
        JOIN FETCH s.representanteLegal
        WHERE s.estado = :estado
        ORDER BY s.fechaSolicitud ASC
        """)
    Page<SolicitudVerificacion> findByEstado(@Param("estado") EstadoSolicitud estado, Pageable pageable);

    @Query("""
        SELECT COUNT(s) > 0 FROM SolicitudVerificacion s
        WHERE s.representanteLegal.id = :representanteId
          AND s.estado = :estado
        """)
    boolean existsByRepresentanteIdAndEstado(@Param("representanteId") Long representanteId,
                                             @Param("estado") EstadoSolicitud estado);

    @Query("""
        SELECT COUNT(s) > 0 FROM SolicitudVerificacion s
        WHERE s.correoEmpresarial = :correo
          AND s.estado IN ('PENDIENTE', 'APROBADA')
        """)
    boolean existsByCorreoEmpresarial(@Param("correo") String correo);
}