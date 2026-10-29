package com.eventhive.app.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.model.InvitacionRol;

public interface InvitacionRolRepository extends JpaRepository<InvitacionRol, Long> {

    Optional<InvitacionRol> findByTokenHashAndFechaAceptacionIsNull(String tokenHash);

    Optional<InvitacionRol> findByTokenHashAndFechaAceptacionIsNullAndFechaRechazoIsNull(String tokenHash);

    boolean existsByCorreoInvitadoIgnoreCaseAndFechaAceptacionIsNullAndFechaExpiracionAfter(
            String correoInvitado, LocalDateTime now);

    boolean existsByCorreoInvitadoIgnoreCaseAndFechaAceptacionIsNullAndFechaRechazoIsNullAndFechaExpiracionAfter(
            String correoInvitado, LocalDateTime now);

    Optional<InvitacionRol> findByCorreoInvitadoIgnoreCaseAndFechaAceptacionIsNullAndFechaExpiracionAfter(
            String correoInvitado, LocalDateTime now);

    Page<InvitacionRol> findByInvitadoPorIdOrderByFechaInvitacionDesc(Long invitadoPorId, Pageable pageable);

    @Query(value = """
        SELECT i FROM InvitacionRol i
        LEFT JOIN FETCH i.organizacion
        WHERE i.organizacion.id = :organizacionId
        ORDER BY i.fechaInvitacion DESC
        """,
        countQuery = """
        SELECT COUNT(i.id) FROM InvitacionRol i
        WHERE i.organizacion.id = :organizacionId
        """)
    Page<InvitacionRol> findByOrganizacionIdOrderByFechaInvitacionDesc(@Param("organizacionId") Long organizacionId, Pageable pageable);
}
