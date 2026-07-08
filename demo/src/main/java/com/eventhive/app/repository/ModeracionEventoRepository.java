package com.eventhive.app.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.model.ModeracionEvento;

public interface ModeracionEventoRepository extends JpaRepository<ModeracionEvento, Long> {

    // Historial de un evento con el moderador cargado, más reciente primero
    @Query("""
        SELECT m FROM ModeracionEvento m
        JOIN FETCH m.moderador
        WHERE m.evento.id = :eventoId
        ORDER BY m.fecha DESC
        """)
    Page<ModeracionEvento> findByEventoId(@Param("eventoId") Long eventoId, Pageable pageable);
}
