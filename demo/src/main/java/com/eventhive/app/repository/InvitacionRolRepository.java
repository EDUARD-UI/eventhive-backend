package com.eventhive.app.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eventhive.app.model.InvitacionRol;

public interface InvitacionRolRepository extends JpaRepository<InvitacionRol, Long> {

    Optional<InvitacionRol> findByTokenHashAndFechaAceptacionIsNull(String tokenHash);

    boolean existsByCorreoInvitadoIgnoreCaseAndFechaAceptacionIsNullAndFechaExpiracionAfter(
            String correoInvitado, LocalDateTime now);

    Optional<InvitacionRol> findByCorreoInvitadoIgnoreCaseAndFechaAceptacionIsNullAndFechaExpiracionAfter(
            String correoInvitado, LocalDateTime now);

    Page<InvitacionRol> findByInvitadoPorIdOrderByFechaInvitacionDesc(Long invitadoPorId, Pageable pageable);

    Page<InvitacionRol> findByOrganizacionIdOrderByFechaInvitacionDesc(Long organizacionId, Pageable pageable);
}
