package com.eventhive.app.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.enums.EstadoPosicionamiento;
import com.eventhive.app.model.PosicionamientoEvento;

public interface PosicionamientoEventoRepository extends JpaRepository<PosicionamientoEvento, Long> {

    Optional<PosicionamientoEvento> findByEventoIdAndEstado(Long eventoId, EstadoPosicionamiento estado);

    boolean existsByEventoIdAndEstado(Long eventoId, EstadoPosicionamiento estado);

    @Query("""
        SELECT COUNT(p) > 0 FROM PosicionamientoEvento p
        LEFT JOIN p.plan pl
        WHERE p.evento.id = :eventoId
          AND p.estado = :estado
          AND (p.planPremium = true OR (pl IS NOT NULL AND pl.premium = true))
        """)
    boolean existsByEventoIdAndEstadoAndPlanPremiumTrue(@Param("eventoId") Long eventoId, @Param("estado") EstadoPosicionamiento estado);

    boolean existsByEventoIdAndEstadoIn(Long eventoId, List<EstadoPosicionamiento> estados);

    List<PosicionamientoEvento> findByEventoIdOrderByFechaContratacionDesc(Long eventoId);

    List<PosicionamientoEvento> findByOrganizadorIdOrderByFechaContratacionDesc(Long organizadorId);

    List<PosicionamientoEvento> findByEstadoOrderByFechaContratacionDesc(EstadoPosicionamiento estado);

    @Query("""
        SELECT COALESCE(SUM(p.precio), 0) FROM PosicionamientoEvento p
        WHERE p.estado = com.eventhive.app.enums.EstadoPosicionamiento.CONFIRMADO
          AND p.evento.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
        """)
    BigDecimal sumarIngresosConfirmados();

    @Query("""
        SELECT COUNT(DISTINCT p.evento.id) FROM PosicionamientoEvento p
        WHERE p.estado = com.eventhive.app.enums.EstadoPosicionamiento.CONFIRMADO
          AND p.evento.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
          AND p.plan IS NOT NULL
        """)
    long contarEventosActivosConPromocionPagada();

    @Query("""
        SELECT p.plan.id, COUNT(DISTINCT p.evento.id)
        FROM PosicionamientoEvento p
        WHERE p.estado = com.eventhive.app.enums.EstadoPosicionamiento.CONFIRMADO
          AND p.plan IS NOT NULL
        GROUP BY p.plan.id
        """)
    List<Object[]> contarEventosPorPlan();
}
