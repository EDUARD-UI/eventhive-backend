package com.example.demo.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Promocion;

@Repository
public interface PromocionRepository extends JpaRepository<Promocion, String> {

    // Busca promoción vigente para un evento en una fecha dada
    @Query("""
        SELECT p FROM Promocion p
        JOIN FETCH p.eventos e
        WHERE e.id = :eventoId
        AND p.fechaInicio <= :hoy
        AND p.fechaFin    >= :hoy
        """)
    Optional<Promocion> findVigenteByEventoId(@Param("eventoId") String eventoId,
                                               @Param("hoy")      LocalDate hoy);

    @Query("SELECT p FROM Promocion p JOIN p.eventos e WHERE e.id = :eventoId")
    List<Promocion> findByEventoId(@Param("eventoId") String eventoId);

    @Query("SELECT COUNT(p) > 0 FROM Promocion p JOIN p.eventos e WHERE e.id = :eventoId")
    boolean existsByEventoId(@Param("eventoId") String eventoId);

    // Comprueba si existe otra promoción para el mismo evento que choque con el rango
    @Query("""
        SELECT COUNT(p) > 0 FROM Promocion p JOIN p.eventos e
        WHERE e.id = :eventoId
        AND p.fechaInicio <= :fin
        AND p.fechaFin   >= :inicio
        AND (:excludeId IS NULL OR p.id <> :excludeId)
        """)
    boolean existsConflictoFechas(@Param("eventoId") String eventoId,
                                   @Param("inicio") LocalDate inicio,
                                   @Param("fin") LocalDate fin,
                                   @Param("excludeId") String excludeId);

    // Para el panel de organizador
    @Query("""
        SELECT DISTINCT p FROM Promocion p
        JOIN FETCH p.eventos e
        WHERE e.organizador.id = :organizadorId
        """)
    Page<Promocion> findByOrganizadorId(@Param("organizadorId") String organizadorId, Pageable pageable);
}