package com.eventhive.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eventhive.app.model.Organizacion;

public interface OrganizacionRepository extends JpaRepository<Organizacion, Long> {

    // Evita dos organizaciones con el mismo NIT/CC
    boolean existsByNit(String nit);
}
