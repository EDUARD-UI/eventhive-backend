package com.eventhive.app.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.model.Evento;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        """)
    Page<Evento> findAllConReferencias(Pageable pageable);

    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        WHERE e.id = :id
        """)
    Optional<Evento> findByIdConReferencias(@Param("id") Long id);

    // -- Queries con filtro de visibilidad por fechaPublicacion y horasAnticipacion --

    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        WHERE e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
          AND e.fechaPublicacion <= :fechaLimite
        """)
    Page<Evento> findPublicadosVisibles(
            @Param("fechaLimite") LocalDateTime fechaLimite,
            Pageable pageable);

    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria c
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        WHERE c.id = :categoriaId
          AND e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
          AND e.fechaPublicacion <= :fechaLimite
        """)
    Page<Evento> findByCategoriaVisibles(
            @Param("categoriaId") Long categoriaId,
            @Param("fechaLimite") LocalDateTime fechaLimite,
            Pageable pageable);

    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        WHERE LOWER(e.titulo) LIKE LOWER(CONCAT('%', :titulo, '%'))
          AND e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
          AND e.fechaPublicacion <= :fechaLimite
        """)
    Page<Evento> findByTituloVisibles(
            @Param("titulo") String titulo,
            @Param("fechaLimite") LocalDateTime fechaLimite,
            Pageable pageable);

    // -- Queries sin filtro de visibilidad (organizador y admin) --

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

    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        WHERE e.estado = :estado
        """)
    Page<Evento> findByEstadoConReferencias(
            @Param("estado") com.eventhive.app.enums.EstadoEvento estado,
            Pageable pageable);

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

    List<Evento> findByOrganizadorId(Long organizadorId);

    List<Evento> findByOrganizadorIdOrderByFechaCreacionDesc(Long organizadorId);

    long countByCategoriaId(Long categoriaId);
    long countByEstado(com.eventhive.app.enums.EstadoEvento estado);
    long countByOrganizadorId(Long organizadorId);

    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.organizador o
        WHERE e.fecha = :fecha
          AND e.estado = :estado
        """)
    List<Evento> findByFechaAndEstado(
            @Param("fecha") java.time.LocalDate fecha,
            @Param("estado") com.eventhive.app.enums.EstadoEvento estado);
}