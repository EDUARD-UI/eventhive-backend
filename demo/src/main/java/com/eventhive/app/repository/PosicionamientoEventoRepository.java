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

    // Obtiene el registro de posicionamiento de un evento para un estado determinado.
    Optional<PosicionamientoEvento> findByEventoIdAndEstado(Long eventoId, EstadoPosicionamiento estado);

    // Verifica si un evento cuenta con un registro de posicionamiento en el estado indicado.
    boolean existsByEventoIdAndEstado(Long eventoId, EstadoPosicionamiento estado);

    // Verifica si un evento cuenta con posicionamiento confirmado bajo un plan catalogado como prémium.
    @Query("""
        SELECT COUNT(p) > 0 FROM PosicionamientoEvento p
        LEFT JOIN p.plan pl
        WHERE p.evento.id = :eventoId
          AND p.estado = :estado
          AND (p.planPremium = true OR (pl IS NOT NULL AND pl.premium = true))
        """)
    boolean existsByEventoIdAndEstadoAndPlanPremiumTrue(@Param("eventoId") Long eventoId, @Param("estado") EstadoPosicionamiento estado);

    // Verifica si un evento tiene un posicionamiento registrado en cualquiera de los estados provistos.
    boolean existsByEventoIdAndEstadoIn(Long eventoId, List<EstadoPosicionamiento> estados);

    // Obtiene el historial completo de posicionamientos de un evento ordenado cronológicamente descendente.
    List<PosicionamientoEvento> findByEventoIdOrderByFechaContratacionDesc(Long eventoId);

    // Obtiene los posicionamientos contratados por un organizador ordenados por fecha descendente.
    List<PosicionamientoEvento> findByOrganizadorIdOrderByFechaContratacionDesc(Long organizadorId);

    // Obtiene los posicionamientos registrados bajo un estado ordenados por fecha de contratación descendente.
    List<PosicionamientoEvento> findByEstadoOrderByFechaContratacionDesc(EstadoPosicionamiento estado);

    // Calcula la suma total de ingresos generados por la contratación confirmada de planes de posicionamiento.
    @Query("""
        SELECT COALESCE(SUM(p.precio), 0) FROM PosicionamientoEvento p
        WHERE p.estado = com.eventhive.app.enums.EstadoPosicionamiento.CONFIRMADO
        """)
    BigDecimal sumarIngresosConfirmados();

    // Cuenta los eventos actualmente publicados que cuentan con un plan de posicionamiento confirmado.
    @Query("""
        SELECT COUNT(DISTINCT p.evento.id) FROM PosicionamientoEvento p
        WHERE p.estado = com.eventhive.app.enums.EstadoPosicionamiento.CONFIRMADO
          AND p.evento.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
          AND p.plan IS NOT NULL
        """)
    long contarEventosActivosConPromocionPagada();

    // Obtiene la cantidad de eventos confirmados agrupada por cada identificador de plan de promoción.
    @Query("""
        SELECT p.plan.id, COUNT(DISTINCT p.evento.id)
        FROM PosicionamientoEvento p
        WHERE p.estado = com.eventhive.app.enums.EstadoPosicionamiento.CONFIRMADO
          AND p.plan IS NOT NULL
        GROUP BY p.plan.id
        """)
    List<Object[]> contarEventosPorPlan();
}
