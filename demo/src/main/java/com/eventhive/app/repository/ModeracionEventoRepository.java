package com.eventhive.app.repository;

import com.eventhive.app.enums.EstadoEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.model.ModeracionEvento;

public interface ModeracionEventoRepository extends JpaRepository<ModeracionEvento, Long> {

    // Obtiene el historial de moderaciones de un evento de forma paginada ordenado por fecha descendente.
    @Query(value = """
    SELECT m FROM ModeracionEvento m
    LEFT JOIN FETCH m.moderador
    JOIN FETCH m.evento
    WHERE m.evento.id = :eventoId
    ORDER BY m.fecha DESC
    """,
    countQuery = """
    SELECT COUNT(m.id) FROM ModeracionEvento m
    WHERE m.evento.id = :eventoId
    """)
    Page<ModeracionEvento> findByEventoId(@Param("eventoId") Long eventoId, Pageable pageable);

    // Verifica si un evento tiene registros de moderación.
    boolean existsByEventoId(Long eventoId);

    // Cuenta el total de revisiones de moderación ejecutadas por un usuario moderador.
    long countByModeradorId(Long moderadorId);

    // Cuenta las revisiones ejecutadas por un moderador cuyo veredicto coincidió con el estado indicado.
    long countByModeradorIdAndEstadoResultante(
            Long moderadorId,
            EstadoEvento estadoResultante);
}
