package com.eventhive.app.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.eventhive.app.model.Organizacion;

public interface OrganizacionRepository extends JpaRepository<Organizacion, Long> {

    // Evita dos organizaciones con el mismo NIT/CC
    boolean existsByNit(String nit);

    // Top: primero por nivel de confianza, luego por cantidad de seguidores
    @Query("SELECT o FROM Organizacion o ORDER BY o.nivel DESC, o.totalSeguidores DESC")
    Page<Organizacion> findTopOrganizaciones(Pageable pageable);

    // Verifica si ya existe una organización con ese correo de contacto.
    boolean existsByCorreoContacto(String correoContacto);
}
