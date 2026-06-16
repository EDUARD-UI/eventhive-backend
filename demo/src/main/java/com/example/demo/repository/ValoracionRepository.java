package com.example.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Valoracion;

@Repository
public interface ValoracionRepository extends JpaRepository<Valoracion, String> {

    @Query("""
        SELECT v FROM Valoracion v
        JOIN FETCH v.organizador
        WHERE v.cliente.id = :clienteId
        ORDER BY v.id DESC
        """)
    // Devuelve valoraciones de un cliente incluyendo el organizador
    Page<Valoracion> findByClienteIdConOrganizador(@Param("clienteId") String clienteId, Pageable pageable);

    @Query("""
        SELECT v FROM Valoracion v
        JOIN FETCH v.cliente
        WHERE v.organizador.id = :organizadorId
        ORDER BY v.id DESC
        """)
    // Devuelve valoraciones de un organizador incluyendo el cliente
    Page<Valoracion> findByOrganizadorIdConCliente(@Param("organizadorId") String organizadorId, Pageable pageable);

    long countByOrganizadorId(String organizadorId);
    long countByClienteId(String clienteId);

    boolean existsByClienteIdAndOrganizadorId(String clienteId, String organizadorId);
}
