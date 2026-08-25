package com.eventhive.app.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.model.Promocion;

public interface PromocionRepository extends JpaRepository<Promocion, Long> {

    // Busca una promoción vigente para un evento en una fecha dada
    @Query("""
        SELECT p FROM Promocion p
        JOIN FETCH p.eventos e
        WHERE e.id = :eventoId
        AND p.fechaInicio <= :hoy
        AND p.fechaFin    >= :hoy
        """)
    Optional<Promocion> findVigenteByEventoId(@Param("eventoId") Long eventoId,
                                               @Param("hoy")      LocalDate hoy);

    // Obtiene las promociones asociadas a un evento
    @Query("SELECT p FROM Promocion p JOIN p.eventos e WHERE e.id = :eventoId")
    List<Promocion> findByEventoId(@Param("eventoId") Long eventoId);

    // Verifica si existen promociones para un evento
    @Query("SELECT COUNT(p) > 0 FROM Promocion p JOIN p.eventos e WHERE e.id = :eventoId")
    boolean existsByEventoId(@Param("eventoId") Long eventoId);

    // Comprueba si existe conflicto de fechas con otra promoción
    @Query("""
        SELECT COUNT(p) > 0 FROM Promocion p JOIN p.eventos e
        WHERE e.id = :eventoId
        AND p.fechaInicio <= :fin
        AND p.fechaFin   >= :inicio
        AND (:excludeId IS NULL OR p.id <> :excludeId)
        """)
    boolean existsConflictoFechas(@Param("eventoId") Long eventoId,
                                   @Param("inicio") LocalDate inicio,
                                   @Param("fin") LocalDate fin,
                                   @Param("excludeId") Long excludeId);

    // Obtiene promociones de un organizador para el panel
    @Query("""
    SELECT DISTINCT p FROM Promocion p
    JOIN FETCH p.eventos e
    WHERE e.organizacion.id = :organizacionId
    """)
    Page<Promocion> findByOrganizacionId(@Param("organizacionId") Long organizacionId, Pageable pageable);
}