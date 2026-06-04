package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Tiquete;

@Repository
public interface TiqueteRepository extends JpaRepository<Tiquete, String> {

    @Query("""
        SELECT t FROM Tiquete t
        JOIN FETCH t.evento e
        JOIN FETCH t.localidad l
        JOIN FETCH e.categoria
        WHERE t.compra.id = :compraId
        """)
    // Devuelve tiquetes de una compra con detalles (evento, localidad, categoria)
    List<Tiquete> findByCompraIdConDetalles(@Param("compraId") String compraId);
}