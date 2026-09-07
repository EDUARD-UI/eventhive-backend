package com.eventhive.app.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eventhive.app.enums.EstadoInvitacion;
import com.eventhive.app.model.InvitacionOrganizacion;

public interface InvitacionOrganizacionRepository extends JpaRepository<InvitacionOrganizacion, Long> {

    // Verifica si existe una invitación por correo y estado.
    boolean existsByCorreoInvitadoAndEstado(String correo, EstadoInvitacion estado);

    // Lista invitaciones de un correo y estado de forma paginada.
    Page<InvitacionOrganizacion> findByCorreoInvitadoAndEstado(String correo, EstadoInvitacion estado, Pageable pageable);

    // Lista invitaciones pertenecientes a una organización.
    Page<InvitacionOrganizacion> findByOrganizacionId(Long organizacionId, Pageable pageable);
}