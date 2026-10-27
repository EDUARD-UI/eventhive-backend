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

    // Obtiene la sugerencia de ascenso de nivel de una organización en el estado indicado.
    Optional<SugerenciaAscenso> findByOrganizacionIdAndEstado(Long organizacionId, EstadoSolicitud estado);

    // Verifica si ya existe una sugerencia de ascenso para una organización en el estado indicado.
    boolean existsByOrganizacionIdAndEstado(Long organizacionId, EstadoSolicitud estado);

    // Obtiene las sugerencias de ascenso filtradas por estado con la organización precargada para revisión.
    @Query(value = """
        SELECT s FROM SugerenciaAscenso s
        JOIN FETCH s.organizacion
        WHERE s.estado = :estado
        """,
        countQuery = """
        SELECT COUNT(s.id) FROM SugerenciaAscenso s
        WHERE s.estado = :estado
        """)
    Page<SugerenciaAscenso> findByEstadoConOrganizacion(@Param("estado") EstadoSolicitud estado, Pageable pageable);
}
