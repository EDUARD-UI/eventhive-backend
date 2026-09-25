package com.eventhive.app.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eventhive.app.model.PalabraProhibida;

public interface PalabraProhibidaRepository extends JpaRepository<PalabraProhibida, Long> {

    // Usado por el motor de reglas: solo trae las activas, una sola vez por evaluación.
    List<PalabraProhibida> findByActivaTrue();

    boolean existsByPalabraIgnoreCase(String palabra);

    Page<PalabraProhibida> findAll(Pageable pageable);
}