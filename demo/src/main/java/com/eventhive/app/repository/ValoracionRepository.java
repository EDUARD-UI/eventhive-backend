package com.eventhive.app.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.model.Valoracion;

public interface ValoracionRepository extends JpaRepository<Valoracion, Long> {

    // Obtiene valoraciones de un cliente con el organizador cargado
    @Query("""
        SELECT v FROM Valoracion v
        JOIN FETCH v.organizador
        WHERE v.cliente.id = :clienteId
        ORDER BY v.id DESC
        """)
    Page<Valoracion> findByClienteIdConOrganizador(@Param("clienteId") Long clienteId, Pageable pageable);

    // Obtiene valoraciones de un organizador con el cliente cargado
    @Query("""
        SELECT v FROM Valoracion v
        JOIN FETCH v.cliente
        WHERE v.organizador.id = :organizadorId
        ORDER BY v.id DESC
        """)
    Page<Valoracion> findByOrganizadorIdConCliente(@Param("organizadorId") Long organizadorId, Pageable pageable);

    // Calcula el promedio de calificaciones de un organizador
    @Query("SELECT AVG(v.calificacion) FROM Valoracion v WHERE v.organizador.id = :organizadorId")
    double calcularPromedioByOrganizadorId(@Param("organizadorId") Long organizadorId);

    // Cuenta las valoraciones de un organizador
    long countByOrganizadorId(Long organizadorId);

    // Cuenta las valoraciones de un cliente
    long countByClienteId(Long clienteId);

    // Verifica si ya existe una valoración entre cliente y organizador
    boolean existsByClienteIdAndOrganizadorId(Long clienteId, Long organizadorId);
}