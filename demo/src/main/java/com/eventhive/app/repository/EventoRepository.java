package com.eventhive.app.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.model.Evento;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    // Busca eventos publicados visibles para la vista pública
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        WHERE e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
        """)
    Page<Evento> findPublicadosVisibles(Pageable pageable);

    // Busca eventos publicados de una categoría específica
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria c
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        WHERE c.id = :categoriaId
          AND e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
        """)
    Page<Evento> findByCategoriaVisibles(
            @Param("categoriaId") Long categoriaId,
            Pageable pageable);

    // Busca eventos publicados cuyo título coincide con el texto indicado
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        WHERE LOWER(e.titulo) LIKE LOWER(CONCAT('%', :titulo, '%'))
          AND e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
        """)
    Page<Evento> findByTituloVisibles(
            @Param("titulo") String titulo,
            Pageable pageable);

    // --- Consultas de organizador y admin (sin filtro de visibilidad) ---

    // Obtiene todos los eventos con sus referencias cargadas
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        """)
    Page<Evento> findAllConReferencias(Pageable pageable);

    // Busca un evento por id con sus referencias cargadas
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        WHERE e.id = :id
        """)
    Optional<Evento> findByIdConReferencias(@Param("id") Long id);

    // Obtiene los eventos de un organizador con sus referencias cargadas
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        WHERE o.id = :organizadorId
        ORDER BY e.fechaCreacion DESC
        """)
    Page<Evento> findByOrganizadorIdConReferencias(
            @Param("organizadorId") Long organizadorId,
            Pageable pageable);

    // Busca eventos de un organizador cuyo título coincide con el texto indicado
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

    // Obtiene eventos filtrados por estado con sus referencias cargadas
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        WHERE e.estado = :estado
        """)
    Page<Evento> findByEstadoConReferencias(
            @Param("estado") EstadoEvento estado,
            Pageable pageable);

    // Busca eventos cuyo título coincide con el texto indicado
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        WHERE LOWER(e.titulo) LIKE LOWER(CONCAT('%', :titulo, '%'))
        """)
    Page<Evento> findByTituloConReferencias(
            @Param("titulo") String titulo,
            Pageable pageable);

    // Busca eventos por fecha y estado
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.organizador o
        WHERE e.fecha = :fecha
          AND e.estado = :estado
        """)
    List<Evento> findByFechaAndEstado(
            @Param("fecha") LocalDate fecha,
            @Param("estado") EstadoEvento estado);

    // Cuenta eventos por categoría para el panel de administración
    long countByCategoriaId(Long categoriaId);

    // Cuenta eventos por estado para el panel de administración
    long countByEstado(EstadoEvento estado);

    // Cuenta eventos por organizador para el panel de administración
    long countByOrganizadorId(Long organizadorId);
}