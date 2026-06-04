package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Evento;

@Repository
public interface EventoRepository extends JpaRepository<Evento, String> {

    // Devuelve página de eventos con sus referencias (categoria, estado, organizador y rol)
    @Query("""
        SELECT e FROM Evento e JOIN FETCH e.categoria JOIN FETCH e.estado JOIN FETCH e.organizador o JOIN FETCH o.rol
        """)
    Page<Evento> findAllConReferencias(Pageable pageable);

    // Devuelve un evento por id junto con sus referencias
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.estado
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        WHERE e.id = :id
        """)
    Optional<Evento> findByIdConReferencias(@Param("id") String id);

    // Lista eventos de un organizador con sus referencias
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.estado
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        WHERE o.id = :organizadorId
        """)
    Page<Evento> findByOrganizadorIdConReferencias(@Param("organizadorId") String organizadorId, Pageable pageable);

    // Lista eventos de una categoria con sus referencias
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria c
        JOIN FETCH e.estado
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        WHERE c.id = :categoriaId
        """)
    Page<Evento> findByCategoriaIdConReferencias(@Param("categoriaId") String categoriaId, Pageable pageable);

    // Lista eventos por estado con sus referencias
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.estado est
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        WHERE est.id = :estadoId
        """)
    Page<Evento> findByEstadoIdConReferencias(@Param("estadoId") String estadoId, Pageable pageable);

    // Busca eventos por título (contains) y trae referencias
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.estado
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        WHERE LOWER(e.titulo) LIKE LOWER(CONCAT('%', :titulo, '%'))
        """)
    Page<Evento> findByTituloConReferencias(@Param("titulo") String titulo, Pageable pageable);

    // Busca eventos de un organizador por título y trae referencias
    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.estado
        JOIN FETCH e.organizador o
        JOIN FETCH o.rol
        WHERE o.id = :organizadorId
        AND LOWER(e.titulo) LIKE LOWER(CONCAT('%', :titulo, '%'))
        """)
    Page<Evento> findByOrganizadorIdAndTituloConReferencias(
            @Param("organizadorId") String organizadorId,
            @Param("titulo") String titulo,
            Pageable pageable);

    List<Evento> findByOrganizadorId(String organizadorId);

    long countByCategoriaId(String categoriaId);
    long countByEstadoId(String estadoId);
    long countByOrganizadorId(String organizadorId);
}