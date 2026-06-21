package com.example.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Compra;


public interface CompraRepository extends JpaRepository<Compra, Long> {

    // Devuelve compras de un cliente con items y detalles (evento, localidad)
    @Query("""
        SELECT DISTINCT c FROM Compra c
        JOIN FETCH c.cliente
        LEFT JOIN FETCH c.items i
        LEFT JOIN FETCH i.evento
        LEFT JOIN FETCH i.localidad
        WHERE c.cliente.id = :clienteId
        ORDER BY c.fechaCompra DESC
        """)
    Page<Compra> findByClienteIdConItems(@Param("clienteId") Long clienteId, Pageable pageable);
}