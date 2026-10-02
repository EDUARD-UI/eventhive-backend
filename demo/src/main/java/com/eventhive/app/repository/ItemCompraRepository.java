package com.eventhive.app.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.dto.response.TopEventoVentasDTO;
import com.eventhive.app.dto.response.TopOrganizacionVentasDTO;
import com.eventhive.app.model.ItemCompra;

public interface ItemCompraRepository extends JpaRepository<ItemCompra, Long> {

    // Top de eventos por entradas vendidas (desempate por valor de ventas). Usar PageRequest.of(0, 5).
    @Query("""
        SELECT new com.eventhive.app.dto.response.TopEventoVentasDTO(
            e.id, e.titulo, o.id, o.razonSocial,
            SUM(i.cantidad), SUM(i.cantidad * i.precioUnitario))
        FROM ItemCompra i
        JOIN i.compra c
        JOIN i.evento e
        JOIN e.organizacion o
        WHERE c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
        GROUP BY e.id, e.titulo, o.id, o.razonSocial
        ORDER BY SUM(i.cantidad) DESC, SUM(i.cantidad * i.precioUnitario) DESC, e.id ASC
        """)
    List<TopEventoVentasDTO> topEventosPorVentas(Pageable pageable);

    // Top de organizaciones por entradas vendidas. Usar PageRequest.of(0, 5).
    @Query("""
        SELECT new com.eventhive.app.dto.response.TopOrganizacionVentasDTO(
            o.id, o.razonSocial,
            SUM(i.cantidad), SUM(i.cantidad * i.precioUnitario))
        FROM ItemCompra i
        JOIN i.compra c
        JOIN i.evento e
        JOIN e.organizacion o
        WHERE c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
        GROUP BY o.id, o.razonSocial
        ORDER BY SUM(i.cantidad) DESC, SUM(i.cantidad * i.precioUnitario) DESC, o.id ASC
        """)
    List<TopOrganizacionVentasDTO> topOrganizacionesPorVentas(Pageable pageable);

    // Entradas compradas por un usuario (compras CONFIRMADAS)
    @Query("""
        SELECT SUM(i.cantidad) FROM ItemCompra i
        WHERE i.compra.cliente.id = :usuarioId
          AND i.compra.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
        """)
    Long contarEntradasCompradas(@Param("usuarioId") Long usuarioId);

    // Eventos distintos en los que el usuario tiene compras CONFIRMADAS
    @Query("""
        SELECT COUNT(DISTINCT i.evento.id) FROM ItemCompra i
        WHERE i.compra.cliente.id = :usuarioId
          AND i.compra.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
        """)
    long contarEventosComprados(@Param("usuarioId") Long usuarioId);

    // Total de boletas vendidas por una organización (compras CONFIRMADAS)
    @Query("""
        SELECT COALESCE(SUM(i.cantidad), 0)
        FROM ItemCompra i
        JOIN i.compra c
        JOIN i.evento e
        WHERE e.organizacion.id = :organizacionId
          AND c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
        """)
    long contarBoletasVendidasPorOrganizacion(@Param("organizacionId") Long organizacionId);

    // Total de ingresos generados por una organización (compras CONFIRMADAS, sin deducir comisión)
    @Query("""
        SELECT COALESCE(SUM(i.cantidad * i.precioUnitario), 0)
        FROM ItemCompra i
        JOIN i.compra c
        JOIN i.evento e
        WHERE e.organizacion.id = :organizacionId
          AND c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
        """)
    java.math.BigDecimal sumarIngresosPorOrganizacion(@Param("organizacionId") Long organizacionId);

    // Boletas vendidas agrupadas por localidad para un conjunto de localidades
    @Query("""
        SELECT i.localidad.id, COALESCE(SUM(i.cantidad), 0)
        FROM ItemCompra i
        JOIN i.compra c
        WHERE i.localidad.id IN :localidadIds
          AND c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
        GROUP BY i.localidad.id
        """)
    List<Object[]> contarVentasPorLocalidadIds(@Param("localidadIds") List<Long> localidadIds);
}
