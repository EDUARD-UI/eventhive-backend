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

    // Obtiene las solicitudes de verificación de un representante legal ordenadas por fecha descendente.
    @Query("""
        SELECT s FROM SolicitudVerificacion s
        JOIN FETCH s.representanteLegal
        WHERE s.representanteLegal.id = :representanteId
        ORDER BY s.fechaSolicitud DESC
        """)
    java.util.List<SolicitudVerificacion> findAllByRepresentanteIdOrderByFechaSolicitudDesc(@Param("representanteId") Long representanteId);

    // Obtiene la solicitud de verificación más reciente radicada por un representante legal.
    Optional<SolicitudVerificacion> findFirstByRepresentanteLegalIdOrderByFechaSolicitudDesc(Long representanteId);

    // Método por defecto que obtiene la solicitud de verificación más reciente de un representante legal.
    default Optional<SolicitudVerificacion> findFirstByRepresentanteId(Long representanteId) {
        return findFirstByRepresentanteLegalIdOrderByFechaSolicitudDesc(representanteId);
    }

    // Obtiene las solicitudes de verificación en un estado determinado ordenadas por antigüedad con representante precargado.
    @Query(value = """
        SELECT s FROM SolicitudVerificacion s
        JOIN FETCH s.representanteLegal
        WHERE s.estado = :estado
        ORDER BY s.fechaSolicitud ASC
        """,
        countQuery = """
        SELECT COUNT(s.id) FROM SolicitudVerificacion s
        WHERE s.estado = :estado
        """)
    Page<SolicitudVerificacion> findByEstado(@Param("estado") EstadoSolicitud estado, Pageable pageable);

    // Busca una solicitud de verificación precargando el representante legal y el administrador evaluador.
    @Query("""
        SELECT s FROM SolicitudVerificacion s
        LEFT JOIN FETCH s.representanteLegal
        LEFT JOIN FETCH s.administradorQueResolvi
        WHERE s.id = :id
        """)
    Optional<SolicitudVerificacion> findByIdConDetalles(@Param("id") Long id);

    // Cuenta la cantidad de solicitudes de verificación que se encuentran en un estado específico.
    long countByEstado(EstadoSolicitud estado);

    // Verifica si un representante cuenta con una solicitud radicada en el estado indicado.
    @Query("""
        SELECT COUNT(s) > 0 FROM SolicitudVerificacion s
        WHERE s.representanteLegal.id = :representanteId
          AND s.estado = :estado
        """)
    boolean existsByRepresentanteIdAndEstado(@Param("representanteId") Long representanteId,
                                             @Param("estado") EstadoSolicitud estado);
}