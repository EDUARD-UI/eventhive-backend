package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.ItemCompra;

@Repository
public interface ItemCompraRepository extends JpaRepository<ItemCompra, Long> {

    @Query("""
        SELECT i FROM ItemCompra i
        JOIN FETCH i.evento
        JOIN FETCH i.localidad
        WHERE i.compra.id = :compraId
        """)
    // Devuelve items de una compra con evento y localidad
    List<ItemCompra> findByCompraIdConDetalles(@Param("compraId") Long compraId);
}