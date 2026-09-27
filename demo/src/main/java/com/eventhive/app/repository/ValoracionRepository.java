package com.eventhive.app.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.model.Valoracion;

public interface ValoracionRepository extends JpaRepository<Valoracion, Long> {

    // Calcula el promedio de calificación de una organización.
    @Query("SELECT AVG(v.calificacion) FROM Valoracion v WHERE v.organizacion.id = :organizacionId")
    double calcularPromedioByOrganizacionId(@Param("organizacionId") Long organizacionId);

    // Cuenta las valoraciones de una organización.
    long countByOrganizacionId(Long organizacionId);

    // Verifica si un cliente ya valoró una organización.
    boolean existsByClienteIdAndOrganizacionId(Long clienteId, Long organizacionId);

    Optional<Valoracion> findByClienteIdAndOrganizacionId(Long clienteId, Long organizacionId);

    // Lista valoraciones de un cliente con la organización cargada.
    @Query(value = """
        SELECT v FROM Valoracion v
        JOIN FETCH v.organizacion
        JOIN FETCH v.cliente
        WHERE v.cliente.id = :clienteId
        ORDER BY v.id DESC
        """,
        countQuery = """
        SELECT COUNT(v) FROM Valoracion v
        WHERE v.cliente.id = :clienteId
        """)
    Page<Valoracion> findByClienteIdConOrganizacion(@Param("clienteId") Long clienteId, Pageable pageable);

    // Lista valoraciones de una organización con el cliente cargado.
    @Query(value = """
        SELECT v FROM Valoracion v
        JOIN FETCH v.cliente
        JOIN FETCH v.organizacion
        WHERE v.organizacion.id = :organizacionId
        ORDER BY v.id DESC
        """,
        countQuery = """
        SELECT COUNT(v) FROM Valoracion v
        WHERE v.organizacion.id = :organizacionId
        """)
    Page<Valoracion> findByOrganizacionIdConCliente(@Param("organizacionId") Long organizacionId, Pageable pageable);
}