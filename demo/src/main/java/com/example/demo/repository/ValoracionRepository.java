package com.example.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Valoracion;

@Repository
public interface ValoracionRepository extends JpaRepository<Valoracion, Long> {

    @Query("""
        SELECT v FROM Valoracion v
        JOIN FETCH v.organizador
        WHERE v.cliente.id = :clienteId
        ORDER BY v.id DESC
        """)
    // Devuelve valoraciones de un cliente incluyendo el organizador
    Page<Valoracion> findByClienteIdConOrganizador(@Param("clienteId") Long clienteId, Pageable pageable);

    @Query("""
        SELECT v FROM Valoracion v
        JOIN FETCH v.cliente
        WHERE v.organizador.id = :organizadorId
        ORDER BY v.id DESC
        """)
    // Devuelve valoraciones de un organizador incluyendo el cliente
    Page<Valoracion> findByOrganizadorIdConCliente(@Param("organizadorId") Long organizadorId, Pageable pageable);

    long countByOrganizadorId(Long organizadorId);
    long countByClienteId(Long clienteId);

    boolean existsByClienteIdAndOrganizadorId(Long clienteId, Long organizadorId);
}
