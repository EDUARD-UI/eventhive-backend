package com.eventhive.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.model.Evento;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    // Devuelve página de eventos con sus referencias (categoria, estado, organizador y rol)
    @Query("""
        SELECT e FROM Evento e JOIN FETCH e.categoria JOIN FETCH e.organizador o JOIN FETCH o.rol
        """)
    Page<Evento> findAllConReferencias(Pageable pageable);

    // Devuelve un evento por id junto con sus referencias
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        WHERE e.id = :id
        """)
    Optional<Evento> findByIdConReferencias(@Param("id") Long id);

    // Lista eventos de un organizador con sus referencias
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        WHERE o.id = :organizadorId
        ORDER BY e.fechaCreacion DESC
        """)
    Page<Evento> findByOrganizadorIdConReferencias(@Param("organizadorId") Long organizadorId, Pageable pageable);

    // Lista eventos de una categoria con sus referencias
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria c
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        WHERE c.id = :categoriaId
        """)
    Page<Evento> findByCategoriaIdConReferencias(@Param("categoriaId") Long categoriaId, Pageable pageable);

    // Lista eventos por estado con sus referencias
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        WHERE e.estado = :estado
        """)
    Page<Evento> findByEstadoConReferencias(@Param("estado") com.eventhive.app.enums.EstadoEvento estado, Pageable pageable);

    // Busca eventos por título (contains) y trae referencias
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        WHERE LOWER(e.titulo) LIKE LOWER(CONCAT('%', :titulo, '%'))
        """)
    Page<Evento> findByTituloConReferencias(@Param("titulo") String titulo, Pageable pageable);

    // Busca eventos de un organizador por título y trae referencias
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        WHERE o.id = :organizadorId
        AND LOWER(e.titulo) LIKE LOWER(CONCAT('%', :titulo, '%'))
        """)
    Page<Evento> findByOrganizadorIdAndTituloConReferencias(
            @Param("organizadorId") Long organizadorId,
            @Param("titulo") String titulo,
            Pageable pageable);

    List<Evento> findByOrganizadorId(Long organizadorId);

    List<Evento> findByOrganizadorIdOrderByFechaCreacionDesc(Long organizadorId);

    long countByCategoriaId(Long categoriaId);

    long countByEstado(com.eventhive.app.enums.EstadoEvento estado);

    long countByOrganizadorId(Long organizadorId);

    // Query necesaria para RecordatorioEventoScheduler
    @Query("""
    SELECT e FROM Evento e
    JOIN FETCH e.organizador o
    WHERE e.fecha = :fecha
    AND e.estado = :estado
    """)
    List<Evento> findByFechaAndEstado(
            @Param("fecha") java.time.LocalDate fecha,
            @Param("estado") com.eventhive.app.enums.EstadoEvento estado
    );
}
