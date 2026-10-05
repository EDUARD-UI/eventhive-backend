package com.eventhive.app.repository;

import com.eventhive.app.enums.EstadoPosicionamiento;
import com.eventhive.app.model.PosicionamientoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PosicionamientoEventoRepository extends JpaRepository<PosicionamientoEvento, Long> {

    // Posicionamiento vigente de un evento (como máximo uno por la regla de unicidad)
    Optional<PosicionamientoEvento> findByEventoIdAndEstado(Long eventoId, EstadoPosicionamiento estado);

    // Evita contratar un segundo posicionamiento sobre el mismo evento
    boolean existsByEventoIdAndEstado(Long eventoId, EstadoPosicionamiento estado);

    boolean existsByEventoIdAndEstadoIn(Long eventoId, List<EstadoPosicionamiento> estados);

    // Historial completo de un evento (confirmados y cancelados)
    List<PosicionamientoEvento> findByEventoIdOrderByFechaContratacionDesc(Long eventoId);

    // Panel del organizador
    List<PosicionamientoEvento> findByOrganizadorIdOrderByFechaContratacionDesc(Long organizadorId);

    // Ingresos de la plataforma por venta de posicionamientos
    @Query("""
        SELECT COALESCE(SUM(p.precio), 0) FROM PosicionamientoEvento p
        WHERE p.estado = com.eventhive.app.enums.EstadoPosicionamiento.CONFIRMADO
        """)
    BigDecimal sumarIngresosConfirmados();
}
