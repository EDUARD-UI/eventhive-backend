package com.eventhive.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.eventhive.app.model.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    // Busca una categoría por su identificador único.
    Optional<Categoria> findById(Long id);

    // Busca una categoría por su nombre exacto.
    Optional<Categoria> findByNombre(String nombre);

    // Verifica si existe una categoría con el nombre indicado ignorando mayúsculas y minúsculas.
    boolean existsByNombreIgnoreCase(String nombre);

    // Obtiene todas las categorías de forma paginada junto con el conteo de eventos publicados asociados.
    @Query(value = """
    SELECT c.id, c.nombre, c.foto, COUNT(e.id)
    FROM Categoria c
    LEFT JOIN Evento e
        ON e.categoria.id = c.id AND e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
    GROUP BY c.id, c.nombre, c.foto
    ORDER BY c.nombre ASC
    """,
    countQuery = """
    SELECT COUNT(c.id) FROM Categoria c
    """)
    Page<Object[]> findAllConCantidadEventos(Pageable pageable);

    // Obtiene la lista de categorías que tienen al menos un evento publicado asociado.
    @Query("""
    SELECT c.id, c.nombre, c.foto, COUNT(e.id)
    FROM Categoria c
    LEFT JOIN Evento e ON e.categoria.id = c.id AND e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
    GROUP BY c.id, c.nombre, c.foto
    HAVING COUNT(e.id) > 0
    ORDER BY c.nombre ASC
    """)
    List<Object[]> findAllConEventos();

    // Obtiene la cantidad de eventos publicados agrupada por categoría.
    @Query("""
    SELECT new com.eventhive.app.dto.response.EventosPorCategoriaDTO(c.id, c.nombre, COUNT(e.id))
    FROM Categoria c
    LEFT JOIN Evento e ON e.categoria.id = c.id AND e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
    GROUP BY c.id, c.nombre
    ORDER BY COUNT(e.id) DESC, c.nombre ASC
    """)
    List<com.eventhive.app.dto.response.EventosPorCategoriaDTO> contarEventosPorCategoria();

    // Obtiene la cantidad de eventos publicados agrupada por categoría para una organización específica.
    @Query("""
    SELECT new com.eventhive.app.dto.response.EventosPorCategoriaDTO(c.id, c.nombre, COUNT(e.id))
    FROM Categoria c
    JOIN Evento e ON e.categoria.id = c.id AND e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
    WHERE e.organizacion.id = :organizacionId
    GROUP BY c.id, c.nombre
    ORDER BY COUNT(e.id) DESC, c.nombre ASC
    """)
    List<com.eventhive.app.dto.response.EventosPorCategoriaDTO> contarEventosPorCategoriaYOrganizacion(@org.springframework.data.repository.query.Param("organizacionId") Long organizacionId);

    // Obtiene las cuatro categorías con mayor cantidad de eventos publicados asociados.
    @Query("""
    SELECT c.id, c.nombre, c.foto, COUNT(e.id)
    FROM Categoria c
    LEFT JOIN Evento e
        ON e.categoria.id = c.id AND e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
    GROUP BY c.id, c.nombre, c.foto
    ORDER BY COUNT(e.id) DESC, c.nombre ASC
    """)
    List<Object[]> findTop4ConCantidadEventos(Pageable pageable);
}