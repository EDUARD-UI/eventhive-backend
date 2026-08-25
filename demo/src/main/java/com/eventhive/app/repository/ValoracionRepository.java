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
        JOIN FETCH v.organizacion
        WHERE v.cliente.id = :clienteId
        ORDER BY v.id DESC
        """)
    Page<Valoracion> findByClienteIdConOrganizacion(@Param("clienteId") Long clienteId, Pageable pageable);

    @Query("""
        SELECT v FROM Valoracion v
        JOIN FETCH v.cliente
        WHERE v.organizacion.id = :organizacionId
        ORDER BY v.id DESC
        """)
    Page<Valoracion> findByOrganizacionIdConCliente(@Param("organizacionId") Long organizacionId, Pageable pageable);

    @Query("SELECT AVG(v.calificacion) FROM Valoracion v WHERE v.organizacion.id = :organizacionId")
    double calcularPromedioByOrganizacionId(@Param("organizacionId") Long organizacionId);

    long countByOrganizacionId(Long organizacionId);

    long countByClienteId(Long clienteId);

    boolean existsByClienteIdAndOrganizacionId(Long clienteId, Long organizacionId);
}