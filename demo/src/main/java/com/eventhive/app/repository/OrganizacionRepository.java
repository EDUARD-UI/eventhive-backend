package com.eventhive.app.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eventhive.app.model.Organizacion;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.domain.Pageable;

public interface OrganizacionRepository extends JpaRepository<Organizacion, Long> {

    // Evita dos organizaciones con el mismo NIT/CC
    boolean existsByNit(String nit);

    // Top: primero por nivel de confianza, luego por cantidad de seguidores
    @Query("SELECT o FROM Organizacion o ORDER BY o.nivel DESC, o.totalSeguidores DESC")
    Page<Organizacion> findTopOrganizaciones(Pageable pageable);
}
