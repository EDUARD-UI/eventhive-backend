package com.eventhive.app.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.enums.EstadoInvitacion;
import com.eventhive.app.model.InvitacionOrganizacion;

public interface InvitacionOrganizacionRepository extends JpaRepository<InvitacionOrganizacion, Long> {

    // Verifica si existe una invitación para una organización con el correo y estado indicados.
    boolean existsByCorreoInvitadoAndEstado(String correo, EstadoInvitacion estado);

    // Obtiene las invitaciones dirigidas a un correo filtradas por estado con la organización y el usuario invitante precargados.
    @Query(value = """
        SELECT i FROM InvitacionOrganizacion i
        JOIN FETCH i.organizacion
        JOIN FETCH i.invitadoPor
        WHERE i.correoInvitado = :correo AND i.estado = :estado
        """,
        countQuery = """
        SELECT COUNT(i.id) FROM InvitacionOrganizacion i
        WHERE i.correoInvitado = :correo AND i.estado = :estado
        """)
    Page<InvitacionOrganizacion> findByCorreoInvitadoAndEstado(@Param("correo") String correo, @Param("estado") EstadoInvitacion estado, Pageable pageable);

    // Obtiene las invitaciones pertenecientes a una organización con sus relaciones precargadas.
    @Query(value = """
        SELECT i FROM InvitacionOrganizacion i
        JOIN FETCH i.organizacion
        JOIN FETCH i.invitadoPor
        WHERE i.organizacion.id = :organizacionId
        """,
        countQuery = """
        SELECT COUNT(i.id) FROM InvitacionOrganizacion i
        WHERE i.organizacion.id = :organizacionId
        """)
    Page<InvitacionOrganizacion> findByOrganizacionId(@Param("organizacionId") Long organizacionId, Pageable pageable);
}