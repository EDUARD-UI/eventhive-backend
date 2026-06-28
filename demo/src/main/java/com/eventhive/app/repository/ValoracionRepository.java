package com.eventhive.app.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.model.Valoracion;

public interface ValoracionRepository extends JpaRepository<Valoracion, Long> {

    @Query("""
        SELECT v FROM Valoracion v
        JOIN FETCH v.organizador
        WHERE v.cliente.id = :clienteId
        ORDER BY v.id DESC
        """)
    Page<Valoracion> findByClienteIdConOrganizador(@Param("clienteId") Long clienteId, Pageable pageable);

    @Query("""
        SELECT v FROM Valoracion v
        JOIN FETCH v.cliente
        WHERE v.organizador.id = :organizadorId
        ORDER BY v.id DESC
        """)
    Page<Valoracion> findByOrganizadorIdConCliente(@Param("organizadorId") Long organizadorId, Pageable pageable);

    @Query("SELECT AVG(v.calificacion) FROM Valoracion v WHERE v.organizador.id = :organizadorId")
    double calcularPromedioByOrganizadorId(@Param("organizadorId") Long organizadorId);

    long countByOrganizadorId(Long organizadorId);
    long countByClienteId(Long clienteId);

    boolean existsByClienteIdAndOrganizadorId(Long clienteId, Long organizadorId);
}