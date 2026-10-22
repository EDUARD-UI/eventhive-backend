package com.eventhive.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eventhive.app.model.PlanPromocion;

public interface PlanPromocionRepository extends JpaRepository<PlanPromocion, Long> {

    Optional<PlanPromocion> findByCodigo(String codigo);

    List<PlanPromocion> findAllByOrderByPrecioAsc();

    List<PlanPromocion> findByActivoTrueOrderByPrecioAsc();

    Page<PlanPromocion> findByActivoTrueOrderByPrecioAsc(Pageable pageable);

    Page<PlanPromocion> findAllByOrderByPrecioAsc(Pageable pageable);
}

