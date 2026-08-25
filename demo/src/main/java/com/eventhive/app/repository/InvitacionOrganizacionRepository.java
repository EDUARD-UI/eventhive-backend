package com.eventhive.app.repository;

import com.eventhive.app.enums.EstadoInvitacion;
import com.eventhive.app.model.InvitacionOrganizacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitacionOrganizacionRepository extends JpaRepository<InvitacionOrganizacion, Long> {

    boolean existsByCorreoInvitadoAndEstado(String correo, EstadoInvitacion estado);

    Page<InvitacionOrganizacion> findByCorreoInvitadoAndEstado(String correo, EstadoInvitacion estado, Pageable pageable);

    Page<InvitacionOrganizacion> findByOrganizacionId(Long organizacionId, Pageable pageable);
}