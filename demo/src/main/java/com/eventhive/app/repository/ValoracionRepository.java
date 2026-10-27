package com.eventhive.app.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.model.Valoracion;

public interface ValoracionRepository extends JpaRepository<Valoracion, Long> {

    // Calcula el promedio numérico de calificación de las valoraciones de una organización.
    @Query("SELECT AVG(v.calificacion) FROM Valoracion v WHERE v.organizacion.id = :organizacionId")
    double calcularPromedioByOrganizacionId(@Param("organizacionId") Long organizacionId);

    // Cuenta el total de valoraciones recibidas por una organización.
    long countByOrganizacionId(Long organizacionId);

    // Verifica si un cliente ya ha registrado una valoración para una organización.
    boolean existsByClienteIdAndOrganizacionId(Long clienteId, Long organizacionId);

    // Obtiene la valoración registrada por un cliente sobre una organización.
    Optional<Valoracion> findByClienteIdAndOrganizacionId(Long clienteId, Long organizacionId);

    // Obtiene las valoraciones emitidas por un cliente con la organización precargada.
    @Query(value = """
        SELECT v FROM Valoracion v
        JOIN FETCH v.organizacion
        JOIN FETCH v.cliente
        WHERE v.cliente.id = :clienteId
        ORDER BY v.id DESC
        """,
        countQuery = """
        SELECT COUNT(v.id) FROM Valoracion v
        WHERE v.cliente.id = :clienteId
        """)
    Page<Valoracion> findByClienteIdConOrganizacion(@Param("clienteId") Long clienteId, Pageable pageable);

    // Obtiene las valoraciones recibidas por una organización con el cliente precargado.
    @Query(value = """
        SELECT v FROM Valoracion v
        JOIN FETCH v.cliente
        JOIN FETCH v.organizacion
        WHERE v.organizacion.id = :organizacionId
        ORDER BY v.id DESC
        """,
        countQuery = """
        SELECT COUNT(v.id) FROM Valoracion v
        WHERE v.organizacion.id = :organizacionId
        """)
    Page<Valoracion> findByOrganizacionIdConCliente(@Param("organizacionId") Long organizacionId, Pageable pageable);
}