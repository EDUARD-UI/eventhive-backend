package com.eventhive.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eventhive.app.model.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    // Busca una categoría por su nombre
    Optional<Categoria> findByNombre(String nombre);

    // Verifica si existe una categoría con ese nombre
    boolean existsByNombre(String nombre);

    // Devuelve las primeras 4 categorías ordenadas por nombre
    List<Categoria> findTop4ByOrderByNombreAsc();
}