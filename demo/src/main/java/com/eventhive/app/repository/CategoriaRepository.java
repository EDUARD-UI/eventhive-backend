package com.eventhive.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.eventhive.app.model.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    // Busca una categoría por su identificador.
    @Override
    Optional<Categoria> findById(Long id);

    // Busca una categoría por su nombre
    Optional<Categoria> findByNombre(String nombre);

    // Verifica si existe una categoría con ese nombre
    boolean existsByNombreIgnoreCase(String nombre);

    // Devuelve las primeras 4 categorías con el mayor numero total de eventos
    @Query("""
    SELECT c
    FROM Categoria c
    LEFT JOIN Evento e
        ON e.categoria.id = c.id
       AND e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
    GROUP BY c.id
    ORDER BY COUNT(e.id) DESC, c.nombre ASC
    """)
    List<Categoria> findTop4PorEventos(Pageable pageable);

    // Obtiene cada categoría con la cantidad de eventos publicados.
    @Query("""
    SELECT c.id, c.nombre, COUNT(e.id)
    FROM Categoria c
    LEFT JOIN Evento e
        ON e.categoria.id = c.id
       AND e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
    GROUP BY c.id, c.nombre
    ORDER BY COUNT(e.id) DESC, c.nombre ASC
    """)
    List<Object[]> obtenerCategoriasConCantidadEventos();
}