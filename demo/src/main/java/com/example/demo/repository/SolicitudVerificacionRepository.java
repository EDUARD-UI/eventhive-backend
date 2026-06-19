package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.enums.EstadoSolicitud;
import com.example.demo.model.SolicitudVerificacion;

@Repository
public interface SolicitudVerificacionRepository extends JpaRepository<SolicitudVerificacion, Long> {

    // Busca la solicitud más reciente de un organizador con sus relaciones cargadas (evita N+1)
    @Query("""
        SELECT s FROM SolicitudVerificacion s
        JOIN FETCH s.organizador
        WHERE s.organizador.id = :organizadorId
        ORDER BY s.fechaSolicitud DESC
        """)
    Optional<SolicitudVerificacion> findFirstByOrganizadorId(@Param("organizadorId") Long organizadorId);

    // Lista solicitudes por estado con el organizador ya cargado (evita N+1 en paginación)
    @Query("""
        SELECT s FROM SolicitudVerificacion s
        JOIN FETCH s.organizador
        WHERE s.estado = :estado
        ORDER BY s.fechaSolicitud ASC
        """)
    Page<SolicitudVerificacion> findByEstado(@Param("estado") EstadoSolicitud estado, Pageable pageable);

    // Verifica si el organizador ya tiene una solicitud en estado PENDIENTE
    @Query("""
        SELECT COUNT(s) > 0 FROM SolicitudVerificacion s
        WHERE s.organizador.id = :organizadorId
        AND s.estado = :estado
        """)
        boolean existsByOrganizadorIdAndEstado(
            @Param("organizadorId") Long organizadorId,
            @Param("estado") EstadoSolicitud estado);

    // Verifica si el correo empresarial ya está en una solicitud pendiente o aprobada
    @Query("""
    SELECT COUNT(s) > 0 FROM SolicitudVerificacion s
    WHERE s.correoEmpresarial = :correo
    AND s.estado IN :estados
    """)
    boolean existsByCorreoEmpresarialAndEstadoIn(
            @Param("correo") String correo,
            @Param("estados") List<EstadoSolicitud> estados);

    // Verifica si el correo empresarial ya existe en cualquier solicitud (evita duplicados)
    @Query("""
    SELECT COUNT(s) > 0 FROM SolicitudVerificacion s
    WHERE s.correoEmpresarial = :correo
    AND s.estado IN ('PENDIENTE', 'APROBADA')
    """)
    boolean existsByCorreoEmpresarial(@Param("correo") String correo);
}
