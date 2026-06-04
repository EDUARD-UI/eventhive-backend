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
        JOIN FETCH v.evento
        WHERE v.cliente.id = :clienteId
        ORDER BY v.id DESC
        """)
    // Devuelve valoraciones de un cliente incluyendo el evento
    Page<Valoracion> findByClienteIdConEvento(@Param("clienteId") String clienteId, Pageable pageable);

    @Query("""
        SELECT v FROM Valoracion v
        JOIN FETCH v.cliente
        WHERE v.evento.id = :eventoId
        ORDER BY v.id DESC
        """)
    // Devuelve valoraciones de un evento incluyendo el cliente
    Page<Valoracion> findByEventoIdConCliente(@Param("eventoId") String eventoId, Pageable pageable);

    long countByEventoId(String eventoId);
    long countByClienteId(String clienteId);

    boolean existsByClienteIdAndEventoId(String clienteId, String eventoId);
}
