package com.eventhive.app.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eventhive.app.model.InvitacionRol;

public interface InvitacionRolRepository extends JpaRepository<InvitacionRol, Long> {

    // Busca una invitación de rol activa y no aceptada mediante su token hash.
    Optional<InvitacionRol> findByTokenHashAndFechaAceptacionIsNull(String tokenHash);

    // Verifica si existe una invitación de rol vigente y pendiente para un correo electrónico.
    boolean existsByCorreoInvitadoIgnoreCaseAndFechaAceptacionIsNullAndFechaExpiracionAfter(
            String correoInvitado, LocalDateTime now);

    // Obtiene una invitación de rol vigente y pendiente para un correo electrónico.
    Optional<InvitacionRol> findByCorreoInvitadoIgnoreCaseAndFechaAceptacionIsNullAndFechaExpiracionAfter(
            String correoInvitado, LocalDateTime now);

    // Obtiene las invitaciones de rol emitidas por un usuario ordenadas por fecha de invitación descendente.
    Page<InvitacionRol> findByInvitadoPorIdOrderByFechaInvitacionDesc(Long invitadoPorId, Pageable pageable);

    // Obtiene las invitaciones de rol asociadas a una organización con la entidad organizativa precargada.
    @org.springframework.data.jpa.repository.Query(value = """
        SELECT i FROM InvitacionRol i
        LEFT JOIN FETCH i.organizacion
        WHERE i.organizacion.id = :organizacionId
        ORDER BY i.fechaInvitacion DESC
        """,
        countQuery = """
        SELECT COUNT(i.id) FROM InvitacionRol i
        WHERE i.organizacion.id = :organizacionId
        """)
    Page<InvitacionRol> findByOrganizacionIdOrderByFechaInvitacionDesc(@org.springframework.data.repository.query.Param("organizacionId") Long organizacionId, Pageable pageable);
}
