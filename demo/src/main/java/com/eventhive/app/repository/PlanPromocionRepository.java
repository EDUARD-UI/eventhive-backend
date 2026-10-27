package com.eventhive.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eventhive.app.model.PlanPromocion;

public interface PlanPromocionRepository extends JpaRepository<PlanPromocion, Long> {

    // Busca un plan de promoción por su código único de identificación.
    Optional<PlanPromocion> findByCodigo(String codigo);

    // Obtiene todos los planes de promoción ordenados por precio en orden ascendente.
    List<PlanPromocion> findAllByOrderByPrecioAsc();

    // Obtiene los planes de promoción activos ordenados por precio en orden ascendente.
    List<PlanPromocion> findByActivoTrueOrderByPrecioAsc();

    // Obtiene los planes de promoción activos de forma paginada ordenados por precio ascendente.
    Page<PlanPromocion> findByActivoTrueOrderByPrecioAsc(Pageable pageable);

    // Obtiene todos los planes de promoción de forma paginada ordenados por precio ascendente.
    Page<PlanPromocion> findAllByOrderByPrecioAsc(Pageable pageable);
}

