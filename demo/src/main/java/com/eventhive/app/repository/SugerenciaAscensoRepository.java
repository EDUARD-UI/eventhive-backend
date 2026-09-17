package com.eventhive.app.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.enums.EstadoSolicitud;
import com.eventhive.app.model.SugerenciaAscenso;

public interface SugerenciaAscensoRepository extends JpaRepository<SugerenciaAscenso, Long> {

    Optional<SugerenciaAscenso> findByOrganizacionIdAndEstado(Long organizacionId, EstadoSolicitud estado);

    // Evita generar una segunda sugerencia mientras la anterior siga pendiente
    boolean existsByOrganizacionIdAndEstado(Long organizacionId, EstadoSolicitud estado);

    // Cola de sugerencias con la organización cargada, para el panel del administrador
    @Query("""
        SELECT s FROM SugerenciaAscenso s
        JOIN FETCH s.organizacion
        WHERE s.estado = :estado
        """)
    Page<SugerenciaAscenso> findByEstadoConOrganizacion(@Param("estado") EstadoSolicitud estado, Pageable pageable);
}
