package com.eventhive.app.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eventhive.app.model.PalabraProhibida;

public interface PalabraProhibidaRepository extends JpaRepository<PalabraProhibida, Long> {

    // Obtiene la lista de palabras prohibidas que se encuentran activas en el filtro de moderación.
    List<PalabraProhibida> findByActivaTrue();

    // Verifica si una palabra ya se encuentra en el listado de términos restringidos sin distinguir mayúsculas.
    boolean existsByPalabraIgnoreCase(String palabra);

    // Obtiene todas las palabras prohibidas registradas de forma paginada.
    Page<PalabraProhibida> findAll(Pageable pageable);
}