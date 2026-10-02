package com.eventhive.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.eventhive.app.model.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    Optional<Categoria> findById(Long id);

    // Busca una categoría por su nombre
    Optional<Categoria> findByNombre(String nombre);

    // Verifica si existe una categoría con ese nombre
    boolean existsByNombreIgnoreCase(String nombre);

    // Devuelve todas las categorías con el número total de eventos relacionados
    @Query(value = """
    SELECT c.id, c.nombre, c.foto, COUNT(e.id)
    FROM Categoria c
    LEFT JOIN Evento e
        ON e.categoria.id = c.id
    GROUP BY c.id, c.nombre, c.foto
    ORDER BY c.nombre ASC
    """,
    countQuery = """
    SELECT COUNT(c) FROM Categoria c
    """)
    Page<Object[]> findAllConCantidadEventos(Pageable pageable);

    @Query("""
    SELECT c.id, c.nombre, c.foto, COUNT(e.id)
    FROM Categoria c
    LEFT JOIN Evento e ON e.categoria.id = c.id
    GROUP BY c.id, c.nombre, c.foto
    HAVING COUNT(e.id) > 0
    ORDER BY c.nombre ASC
    """)
    List<Object[]> findAllConEventos();

    // Cantidad de eventos por categoría (incluye categorías sin eventos con 0)
    @Query("""
    SELECT new com.eventhive.app.dto.response.EventosPorCategoriaDTO(c.id, c.nombre, COUNT(e.id))
    FROM Categoria c
    LEFT JOIN Evento e ON e.categoria.id = c.id
    GROUP BY c.id, c.nombre
    ORDER BY COUNT(e.id) DESC, c.nombre ASC
    """)
    List<com.eventhive.app.dto.response.EventosPorCategoriaDTO> contarEventosPorCategoria();

    // Cantidad de eventos por categoría para una organización específica
    @Query("""
    SELECT new com.eventhive.app.dto.response.EventosPorCategoriaDTO(c.id, c.nombre, COUNT(e.id))
    FROM Categoria c
    JOIN Evento e ON e.categoria.id = c.id
    WHERE e.organizacion.id = :organizacionId
    GROUP BY c.id, c.nombre
    ORDER BY COUNT(e.id) DESC, c.nombre ASC
    """)
    List<com.eventhive.app.dto.response.EventosPorCategoriaDTO> contarEventosPorCategoriaYOrganizacion(@org.springframework.data.repository.query.Param("organizacionId") Long organizacionId);

    // Devuelve las 4 categorías destacadas con el mayor número total de eventos
    @Query("""
    SELECT c.id, c.nombre, c.foto, COUNT(e.id)
    FROM Categoria c
    LEFT JOIN Evento e
        ON e.categoria.id = c.id
    GROUP BY c.id, c.nombre, c.foto
    ORDER BY COUNT(e.id) DESC, c.nombre ASC
    """)
    List<Object[]> findTop4ConCantidadEventos(Pageable pageable);
}