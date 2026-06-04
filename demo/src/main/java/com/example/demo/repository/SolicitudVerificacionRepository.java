package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.SolicitudVerificacion;

@Repository
public interface SolicitudVerificacionRepository extends JpaRepository<SolicitudVerificacion, String> {

    @Query("""
        SELECT s FROM SolicitudVerificacion s
        JOIN FETCH s.organizador
        WHERE s.organizador.id = :organizadorId
        ORDER BY s.fechaSolicitud DESC
        """)
    Optional<SolicitudVerificacion> findFirstByOrganizadorId(@Param("organizadorId") String organizadorId);

    @Query("""
        SELECT s FROM SolicitudVerificacion s
        JOIN FETCH s.organizador
        WHERE s.estado = :estado
        """)
    Page<SolicitudVerificacion> findByEstado(@Param("estado") String estado, Pageable pageable);
}
