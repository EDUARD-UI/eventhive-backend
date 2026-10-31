package com.eventhive.app.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.enums.EstadoInvitacion;
import com.eventhive.app.model.InvitacionRol;

public interface InvitacionRolRepository extends JpaRepository<InvitacionRol, Long> {

    Optional<InvitacionRol> findByTokenHashAndFechaAceptacionIsNull(String tokenHash);

    boolean existsByCorreoInvitadoIgnoreCaseAndFechaAceptacionIsNullAndFechaExpiracionAfter(
            String correoInvitado, LocalDateTime now);

    Optional<InvitacionRol> findByCorreoInvitadoIgnoreCaseAndFechaAceptacionIsNullAndFechaExpiracionAfter(
            String correoInvitado, LocalDateTime now);

    @Query(value = """
        SELECT i FROM InvitacionRol i
        JOIN FETCH i.rolDestino
        LEFT JOIN FETCH i.organizacion
        WHERE i.invitadoPor.id = :invitadoPorId
          AND i.estado IN :estados
        ORDER BY i.fechaInvitacion DESC
        """,
        countQuery = """
        SELECT COUNT(i.id) FROM InvitacionRol i
        WHERE i.invitadoPor.id = :invitadoPorId
          AND i.estado IN :estados
        """)
    Page<InvitacionRol> findByInvitadoPorIdAndEstados(
            @Param("invitadoPorId") Long invitadoPorId,
            @Param("estados") Collection<EstadoInvitacion> estados,
            Pageable pageable);

    @Query(value = """
        SELECT i FROM InvitacionRol i
        JOIN FETCH i.rolDestino
        LEFT JOIN FETCH i.organizacion
        WHERE i.organizacion.id = :organizacionId
          AND i.estado IN :estados
        ORDER BY i.fechaInvitacion DESC
        """,
        countQuery = """
        SELECT COUNT(i.id) FROM InvitacionRol i
        WHERE i.organizacion.id = :organizacionId
          AND i.estado IN :estados
        """)
    Page<InvitacionRol> findByOrganizacionIdAndEstados(
            @Param("organizacionId") Long organizacionId,
            @Param("estados") Collection<EstadoInvitacion> estados,
            Pageable pageable);

    default Page<InvitacionRol> findActivasYExpiradasPorInvitador(Long invitadoPorId, Pageable pageable) {
        return findByInvitadoPorIdAndEstados(invitadoPorId, List.of(EstadoInvitacion.PENDIENTE, EstadoInvitacion.EXPIRADA), pageable);
    }

    default Page<InvitacionRol> findActivasYExpiradasPorOrganizacion(Long organizacionId, Pageable pageable) {
        return findByOrganizacionIdAndEstados(organizacionId, List.of(EstadoInvitacion.PENDIENTE, EstadoInvitacion.EXPIRADA), pageable);
    }
}
