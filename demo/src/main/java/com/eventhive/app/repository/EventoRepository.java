package com.eventhive.app.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.model.Evento;
import com.eventhive.app.repository.projection.EventoMapaProjection;

public interface EventoRepository extends JpaRepository<Evento, Long>, JpaSpecificationExecutor<Evento> {

  // Cuenta los eventos asociados a una categoría.
    long countByCategoriaId(Long categoriaId);

  // Cuenta los eventos que tienen un estado específico.
    long countByEstado(EstadoEvento estado);

  // Cuenta los eventos de una organización.
    long countByOrganizacionId(Long organizacionId);

  // Lista eventos publicados con categoría y organización cargadas.
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizacion o
        WHERE e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
        """)
    Page<Evento> findPublicadosVisibles(Pageable pageable);

    // Lista eventos publicados filtrados por categoría.
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria c
        JOIN FETCH e.organizacion o
        WHERE c.id = :categoriaId
          AND e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
        """)
    Page<Evento> findByCategoriaVisibles(@Param("categoriaId") Long categoriaId, Pageable pageable);

    // Busca eventos publicados por título y fecha.
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizacion o
        WHERE e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
          AND (:titulo IS NULL OR LOWER(e.titulo) LIKE LOWER(CONCAT('%', :titulo, '%')))
          AND (:fecha IS NULL OR e.fecha = :fecha)
        """)
    Page<Evento> findByTituloAndFechaVisibles(@Param("titulo") String titulo,
                                             @Param("fecha") LocalDate fecha,
                                             Pageable pageable);

    // Obtiene eventos publicados para mostrarlos en el mapa.
    @Query(value = """
        SELECT e.id AS id, e.titulo AS titulo, e.descripcion AS descripcion,
               c.nombre AS categoriaNombre,
               ST_Y(e.ubicacion::geometry) AS latitud,
               ST_X(e.ubicacion::geometry) AS longitud
        FROM eventos e
        JOIN categorias c ON c.id = e.categoria_id
        WHERE e.estado = 'PUBLICADO'
          AND (:categoriaId IS NULL OR e.categoria_id = :categoriaId)
          AND (
                CAST(:lat AS double precision) IS NULL
                OR CAST(:lng AS double precision) IS NULL
                OR CAST(:radioMetros AS double precision) IS NULL
                OR ST_DWithin(
                     e.ubicacion,
                     ST_SetSRID(ST_MakePoint(CAST(:lng AS double precision), CAST(:lat AS double precision)), 4326)::geography,
                     CAST(:radioMetros AS double precision)
                   )
              )
        """, nativeQuery = true)
    List<EventoMapaProjection> findParaMapa(@Param("categoriaId") Long categoriaId,
                                            @Param("lat") Double lat,
                                            @Param("lng") Double lng,
                                            @Param("radioMetros") Double radioMetros);

    // Busca un evento por id con sus referencias cargadas.
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizacion o
        WHERE e.id = :id
        """)
    Optional<Evento> findByIdConReferencias(@Param("id") Long id);

    // Usado por el endpoint público: solo expone el evento si está PUBLICADO (bug #2.2)
    // Lista eventos de una organización con sus referencias cargadas.
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizacion o
        WHERE e.id = :id
          AND e.estado = :estado
        """)
    Optional<Evento> findByIdAndEstadoConReferencias(@Param("id") Long id, @Param("estado") EstadoEvento estado);

    // Busca eventos de una organización por título.
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizacion o
        WHERE o.id = :organizacionId
        ORDER BY e.fechaCreacion DESC
        """)
    Page<Evento> findByOrganizacionIdConReferencias(@Param("organizacionId") Long organizacionId, Pageable pageable);

    // Lista eventos filtrados por estado con sus referencias cargadas.
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizacion o
        WHERE o.id = :organizacionId
          AND LOWER(e.titulo) LIKE LOWER(CONCAT('%', :titulo, '%'))
        """)
    Page<Evento> findByOrganizacionIdAndTituloConReferencias(@Param("organizacionId") Long organizacionId,
                                                             @Param("titulo") String titulo,
                                                             Pageable pageable);

    // Busca eventos de una fecha y estado determinados.
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizacion o
        WHERE e.estado = :estado
        """)
    Page<Evento> findByEstadoConReferencias(@Param("estado") EstadoEvento estado, Pageable pageable);

    // Cuenta eventos activos de una organización.
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.organizacion o
        WHERE e.fecha = :fecha
          AND e.estado = :estado
        """)
    List<Evento> findByFechaAndEstado(@Param("fecha") LocalDate fecha, @Param("estado") EstadoEvento estado);

    // Lista eventos vencidos que conservan el estado indicado.
    @Query("""
        SELECT COUNT(e) FROM Evento e
        WHERE e.organizacion.id = :organizacionId
          AND e.estado IN (
              com.eventhive.app.enums.EstadoEvento.PENDIENTE_REVISION,
              com.eventhive.app.enums.EstadoEvento.EN_CORRECCION,
              com.eventhive.app.enums.EstadoEvento.PUBLICADO)
        """)
    long countActivosByOrganizacionId(@Param("organizacionId") Long organizacionId);

    // Lista próximos eventos publicados ordenados por fecha y hora.
    @Query("""
    SELECT e FROM Evento e
    JOIN FETCH e.organizacion
    WHERE e.estado = :estado
      AND (e.fecha < :hoy OR (e.fecha = :hoy AND e.hora < :horaActual))
    """)
    List<Evento> findVencidosYEstado(@Param("hoy") LocalDate hoy,
                                     @Param("horaActual") LocalTime horaActual,
                                     @Param("estado") EstadoEvento estado);

    // Lista eventos anteriores a una fecha con el estado indicado.
    @Query("""
    SELECT e FROM Evento e
    JOIN FETCH e.categoria
    JOIN FETCH e.organizacion
    WHERE e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
      AND (
            e.fecha > :hoy
            OR (e.fecha = :hoy AND e.hora >= :hora)
          )
    ORDER BY e.fecha ASC, e.hora ASC, e.id ASC
    """)
    Page<Evento> findProximosPublicados(
            @Param("hoy") LocalDate hoy,
            @Param("hora") LocalTime hora,
            Pageable pageable);

    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.organizacion
        WHERE e.fecha < :fecha
          AND e.estado = :estado
        """)
    List<Evento> findByFechaAnteriorYEstado(@Param("fecha") LocalDate fecha, @Param("estado") EstadoEvento estado);

    // Lista eventos posteriores a una fecha con el estado indicado.
    List<Evento> findByFechaAfterAndEstado(LocalDate fechaActual, EstadoEvento estadoEvento);
}