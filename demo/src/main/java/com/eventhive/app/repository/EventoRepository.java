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

    // Cuenta los eventos asociados a una categoría específica.
    long countByCategoriaId(Long categoriaId);

    // Cuenta los eventos asociados a una categoría y con un estado determinado.
    long countByCategoriaIdAndEstado(Long categoriaId, EstadoEvento estado);

    // Obtiene la cantidad de eventos agrupada por cada estado registrado en el sistema.
    @Query("SELECT new com.eventhive.app.dto.response.EventoEstadoConteoDTO(e.estado, COUNT(e.id)) FROM Evento e GROUP BY e.estado")
    List<com.eventhive.app.dto.response.EventoEstadoConteoDTO> contarPorEstado();

    // Cuenta la cantidad de eventos que tienen un estado específico.
    long countByEstado(EstadoEvento estado);

    // Cuenta todos los eventos creados por una organización.
    long countByOrganizacionId(Long organizacionId);

    // Cuenta los eventos de una organización que coinciden con un estado específico.
    long countByOrganizacionIdAndEstado(Long organizacionId, EstadoEvento estado);

    // Obtiene los eventos de una organización paginados y ordenados por identificador en orden descendente.
    Page<Evento> findByOrganizacionIdOrderByIdDesc(Long organizacionId, Pageable pageable);

    // Obtiene los eventos publicados visibles en cartelera con su categoría y organización precargadas priorizando los promocionados.
    @Query(value = """
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizacion o
        WHERE e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
        ORDER BY e.promocionado DESC
        """,
        countQuery = """
        SELECT COUNT(e.id) FROM Evento e
        WHERE e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
        """)
    Page<Evento> findPublicadosVisibles(Pageable pageable);

    // Obtiene los eventos con posicionamiento confirmado que se encuentran publicados con referencias precargadas.
    @Query(value = """
        SELECT DISTINCT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizacion o
        JOIN PosicionamientoEvento p ON p.evento = e
        WHERE p.estado = com.eventhive.app.enums.EstadoPosicionamiento.CONFIRMADO
          AND e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
        ORDER BY e.fecha ASC, e.hora ASC, e.id ASC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT e.id) FROM Evento e
        JOIN PosicionamientoEvento p ON p.evento = e
        WHERE p.estado = com.eventhive.app.enums.EstadoPosicionamiento.CONFIRMADO
          AND e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
        """)
    Page<Evento> findDestacadosPublicados(Pageable pageable);

    // Obtiene los eventos publicados y promocionados que disponen de imagen para el carrusel principal.
    @Query(value = """
        SELECT DISTINCT e FROM Evento e
        WHERE e.promocionado = true
          AND e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
          AND e.urlImagenDestacado IS NOT NULL
          AND TRIM(e.urlImagenDestacado) != ''
        ORDER BY e.fecha ASC, e.hora ASC, e.id ASC
        """)
    List<Evento> findCarruselPublicados();

    // Obtiene los eventos publicados de una categoría específica con sus relaciones precargadas.
    @Query(value = """
        SELECT e FROM Evento e
        JOIN FETCH e.categoria c
        JOIN FETCH e.organizacion o
        WHERE c.id = :categoriaId
          AND e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
        ORDER BY e.promocionado DESC
        """,
        countQuery = """
        SELECT COUNT(e.id) FROM Evento e
        WHERE e.categoria.id = :categoriaId
          AND e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
        """)
    Page<Evento> findByCategoriaVisibles(@Param("categoriaId") Long categoriaId, Pageable pageable);

    // Busca eventos publicados filtrados por coincidencia parcial de título y por fecha específica.
    @Query(value = """
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizacion o
        WHERE e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
          AND (:titulo IS NULL OR LOWER(e.titulo) LIKE LOWER(CONCAT('%', :titulo, '%')))
          AND (:fecha IS NULL OR e.fecha = :fecha)
        ORDER BY e.promocionado DESC
        """,
        countQuery = """
        SELECT COUNT(e.id) FROM Evento e
        WHERE e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
          AND (:titulo IS NULL OR LOWER(e.titulo) LIKE LOWER(CONCAT('%', :titulo, '%')))
          AND (:fecha IS NULL OR e.fecha = :fecha)
        """)
    Page<Evento> findByTituloAndFechaVisibles(@Param("titulo") String titulo,
                                             @Param("fecha") LocalDate fecha,
                                             Pageable pageable);

    // Obtiene las coordenadas y datos esenciales de eventos publicados dentro de un radio geográfico o por categoría para visualización en mapa.
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
        ORDER BY e.promocionado DESC, e.fecha ASC, e.hora ASC, e.id ASC
        """, nativeQuery = true)
    List<EventoMapaProjection> findParaMapa(@Param("categoriaId") Long categoriaId,
                                            @Param("lat") Double lat,
                                            @Param("lng") Double lng,
                                            @Param("radioMetros") Double radioMetros);

    // Busca un evento por su identificador precargando categoría, organización y sus localidades asociadas.
    @Query("""
    SELECT e FROM Evento e
    JOIN FETCH e.categoria
    JOIN FETCH e.organizacion o
    LEFT JOIN FETCH e.localidades
    WHERE e.id = :id
    """)
    Optional<Evento> findByIdConReferencias(@Param("id") Long id);

    // Busca un evento por identificador y estado específico precargando sus referencias y localidades.
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizacion o
        LEFT JOIN FETCH e.localidades
        WHERE e.id = :id
          AND e.estado = :estado
        """)
    Optional<Evento> findByIdAndEstadoConReferencias(@Param("id") Long id, @Param("estado") EstadoEvento estado);

    // Obtiene los eventos de una organización ordenados por fecha de creación descendente con referencias precargadas.
    @Query(value = """
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizacion o
        WHERE o.id = :organizacionId
        ORDER BY e.fechaCreacion DESC
        """,
        countQuery = """
        SELECT COUNT(e.id) FROM Evento e
        WHERE e.organizacion.id = :organizacionId
        """)
    Page<Evento> findByOrganizacionIdConReferencias(@Param("organizacionId") Long organizacionId, Pageable pageable);

    // Busca eventos de una organización filtrados por coincidencia en el título con referencias precargadas.
    @Query(value = """
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizacion o
        WHERE o.id = :organizacionId
          AND LOWER(e.titulo) LIKE LOWER(CONCAT('%', :titulo, '%'))
        """,
        countQuery = """
        SELECT COUNT(e.id) FROM Evento e
        WHERE e.organizacion.id = :organizacionId
          AND LOWER(e.titulo) LIKE LOWER(CONCAT('%', :titulo, '%'))
        """)
    Page<Evento> findByOrganizacionIdAndTituloConReferencias(@Param("organizacionId") Long organizacionId,
                                                             @Param("titulo") String titulo,
                                                             Pageable pageable);

    // Obtiene los eventos filtrados por estado con su categoría y organización precargadas.
    @Query(value = """
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizacion o
        WHERE e.estado = :estado
        """,
        countQuery = """
        SELECT COUNT(e.id) FROM Evento e
        WHERE e.estado = :estado
        """)
    Page<Evento> findByEstadoConReferencias(@Param("estado") EstadoEvento estado, Pageable pageable);

    // Obtiene los eventos programados para una fecha específica y con un estado determinado.
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.organizacion o
        WHERE e.fecha = :fecha
          AND e.estado = :estado
        """)
    List<Evento> findByFechaAndEstado(@Param("fecha") LocalDate fecha, @Param("estado") EstadoEvento estado);

    // Cuenta los eventos activos (en revisión, corrección o publicados) pertenecientes a una organización.
    @Query("""
        SELECT COUNT(e.id) FROM Evento e
        WHERE e.organizacion.id = :organizacionId
          AND e.estado IN (
              com.eventhive.app.enums.EstadoEvento.PENDIENTE_REVISION,
              com.eventhive.app.enums.EstadoEvento.EN_CORRECCION,
              com.eventhive.app.enums.EstadoEvento.PUBLICADO)
        """)
    long countActivosByOrganizacionId(@Param("organizacionId") Long organizacionId);

    // Obtiene los eventos cuya fecha y hora ya transcurrieron y mantienen un estado específico.
    @Query("""
    SELECT e FROM Evento e
    JOIN FETCH e.organizacion
    WHERE e.estado = :estado
      AND (e.fecha < :hoy OR (e.fecha = :hoy AND e.hora < :horaActual))
    """)
    List<Evento> findVencidosYEstado(@Param("hoy") LocalDate hoy,
                                     @Param("horaActual") LocalTime horaActual,
                                     @Param("estado") EstadoEvento estado);

    // Cuenta en lote la cantidad de eventos activos agrupados por identificador de organización.
    @Query("""
        SELECT e.organizacion.id, COUNT(e.id)
        FROM Evento e
        WHERE e.organizacion.id IN :organizacionIds
          AND e.estado IN (
              com.eventhive.app.enums.EstadoEvento.PENDIENTE_REVISION,
              com.eventhive.app.enums.EstadoEvento.EN_CORRECCION,
              com.eventhive.app.enums.EstadoEvento.PUBLICADO)
        GROUP BY e.organizacion.id
        """)
    List<Object[]> countActivosByOrganizacionIds(@Param("organizacionIds") List<Long> organizacionIds);

    // Obtiene los próximos eventos publicados a partir de la fecha y hora actual ordenados cronológicamente.
    @Query(value = """
    SELECT e FROM Evento e
    JOIN FETCH e.categoria
    JOIN FETCH e.organizacion
    WHERE e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
      AND (
            e.fecha > :hoy
            OR (e.fecha = :hoy AND e.hora >= :hora)
          )
    ORDER BY e.fecha ASC, e.hora ASC, e.id ASC
    """,
    countQuery = """
    SELECT COUNT(e.id) FROM Evento e
    WHERE e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
      AND (
            e.fecha > :hoy
            OR (e.fecha = :hoy AND e.hora >= :hora)
          )
    """)
    Page<Evento> findProximosPublicados(
            @Param("hoy") LocalDate hoy,
            @Param("hora") LocalTime hora,
            Pageable pageable);

    // Obtiene los eventos cuya fecha es anterior a la indicada y se encuentran en un estado determinado.
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.organizacion
        WHERE e.fecha < :fecha
          AND e.estado = :estado
        """)
    List<Evento> findByFechaAnteriorYEstado(@Param("fecha") LocalDate fecha, @Param("estado") EstadoEvento estado);

    // Obtiene los eventos programados para fechas posteriores a la actual con un estado específico.
    List<Evento> findByFechaAfterAndEstado(LocalDate fechaActual, EstadoEvento estadoEvento);

    // Obtiene el primer evento destacado y publicado programado a partir de la fecha y hora actual.
    @Query("""
        SELECT e FROM Evento e
        WHERE e.promocionado = true
          AND e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
          AND (e.fecha > :hoy OR (e.fecha = :hoy AND e.hora >= :hora))
        ORDER BY e.fecha ASC, e.hora ASC, e.id ASC
        """)
    List<Evento> findPrimerDestacadoProximo(
            @Param("hoy") LocalDate hoy,
            @Param("hora") LocalTime hora,
            Pageable pageable);

    // Obtiene el primer evento publicado y promocionado disponible como alternativa de cartelera.
    @Query("""
        SELECT e FROM Evento e
        WHERE e.promocionado = true
          AND e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
        ORDER BY e.fecha ASC, e.hora ASC, e.id ASC
        """)
    List<Evento> findPrimerDestacado(Pageable pageable);

    // Obtiene los eventos publicados pertenecientes a una organización con referencias precargadas.
    @Query(value = """
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizacion o
        WHERE o.id = :organizacionId
          AND e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
        ORDER BY e.promocionado DESC, e.fecha ASC, e.hora ASC, e.id ASC
        """,
        countQuery = """
        SELECT COUNT(e.id) FROM Evento e
        WHERE e.organizacion.id = :organizacionId
          AND e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
        """)
    Page<Evento> findPublicadosByOrganizacionId(@Param("organizacionId") Long organizacionId, Pageable pageable);

    // Obtiene los eventos publicados a partir de una fecha con sus referencias precargadas ordenados cronológicamente.
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria c
        JOIN FETCH e.organizacion o
        WHERE e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
          AND (e.fecha > :fecha OR (e.fecha = :fecha AND e.hora >= :hora))
        ORDER BY e.fecha ASC, e.hora ASC, e.id ASC
        """)
    List<Evento> findProximosPublicados(@Param("fecha") LocalDate fecha,
                                       @Param("hora") LocalTime hora,
                                       Pageable pageable);

    // Obtiene los eventos publicados por categoría y rango de fechas con referencias precargadas para consultas específicas.
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria c
        JOIN FETCH e.organizacion o
        WHERE e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
          AND (:categoriaNombre IS NULL OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :categoriaNombre, '%')))
          AND (:fechaInicio IS NULL OR e.fecha >= :fechaInicio)
          AND (:fechaFin IS NULL OR e.fecha <= :fechaFin)
        ORDER BY e.fecha ASC, e.hora ASC, e.id ASC
        """)
    List<Evento> findPublicadosFiltroChatbot(@Param("categoriaNombre") String categoriaNombre,
                                            @Param("fechaInicio") LocalDate fechaInicio,
                                            @Param("fechaFin") LocalDate fechaFin,
                                            Pageable pageable);

    // Obtiene todos los eventos publicados con referencias para proveer contexto completo al chatbot.
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria c
        JOIN FETCH e.organizacion o
        WHERE e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
        ORDER BY e.fecha ASC, e.hora ASC, e.id ASC
        """)
    List<Evento> findAllPublicadosParaChatbot(Pageable pageable);
}