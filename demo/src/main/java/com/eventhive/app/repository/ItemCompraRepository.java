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

    // Obtiene el ranking de eventos publicados con mayor número de boletas vendidas e ingresos en compras confirmadas.
    @Query("""
        SELECT new com.eventhive.app.dto.response.TopEventoVentasDTO(
            e.id, e.titulo, o.id, o.razonSocial,
            SUM(i.cantidad), SUM(i.cantidad * i.precioUnitario))
        FROM ItemCompra i
        JOIN i.compra c
        JOIN i.evento e
        JOIN e.organizacion o
        WHERE c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
          AND e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
        GROUP BY e.id, e.titulo, o.id, o.razonSocial
        ORDER BY SUM(i.cantidad) DESC, SUM(i.cantidad * i.precioUnitario) DESC, e.id ASC
        """)
    List<TopEventoVentasDTO> topEventosPorVentas(Pageable pageable);

    // Obtiene el ranking de organizaciones con mayor volumen de ventas en eventos promocionados y compras confirmadas.
    @Query("""
        SELECT new com.eventhive.app.dto.response.TopOrganizacionVentasDTO(
            o.id, o.razonSocial,
            SUM(i.cantidad), SUM(i.cantidad * i.precioUnitario))
        FROM ItemCompra i
        JOIN i.compra c
        JOIN i.evento e
        JOIN e.organizacion o
        WHERE c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
          AND e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
          AND e.promocionado = true
        GROUP BY o.id, o.razonSocial
        ORDER BY SUM(i.cantidad) DESC, SUM(i.cantidad * i.precioUnitario) DESC, o.id ASC
        """)
    List<TopOrganizacionVentasDTO> topOrganizacionesPorVentas(Pageable pageable);

    // Calcula la cantidad total de boletas adquiridas por un usuario en compras confirmadas.
    @Query("""
        SELECT SUM(i.cantidad) FROM ItemCompra i
        JOIN i.compra c
        WHERE c.cliente.id = :usuarioId
          AND c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
        """)
    Long contarEntradasCompradas(@Param("usuarioId") Long usuarioId);

    // Cuenta la cantidad de eventos distintos en los que un usuario registra compras confirmadas.
    @Query("""
        SELECT COUNT(DISTINCT i.evento.id) FROM ItemCompra i
        JOIN i.compra c
        WHERE c.cliente.id = :usuarioId
          AND c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
        """)
    long contarEventosComprados(@Param("usuarioId") Long usuarioId);

    // Calcula el número total de boletas vendidas en eventos de una organización en compras confirmadas.
    @Query("""
        SELECT COALESCE(SUM(i.cantidad), 0)
        FROM ItemCompra i
        JOIN i.compra c
        JOIN i.evento e
        WHERE e.organizacion.id = :organizacionId
          AND c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
        """)
    long contarBoletasVendidasPorOrganizacion(@Param("organizacionId") Long organizacionId);

    // Calcula el número total de boletas vendidas exclusivamente en eventos publicados de una organización.
    @Query("""
        SELECT COALESCE(SUM(i.cantidad), 0)
        FROM ItemCompra i
        JOIN i.compra c
        JOIN i.evento e
        WHERE e.organizacion.id = :organizacionId
          AND e.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
          AND c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
        """)
    long contarBoletasVendidasEventosPublicadosPorOrganizacion(@Param("organizacionId") Long organizacionId);

    // Obtiene la cantidad de boletas vendidas en compras confirmadas agrupada por identificador de evento.
    @Query("""
        SELECT i.evento.id, COALESCE(SUM(i.cantidad), 0)
        FROM ItemCompra i
        JOIN i.compra c
        WHERE i.evento.id IN :eventoIds
          AND c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
        GROUP BY i.evento.id
        """)
    List<Object[]> contarVentasPorEventoIds(@Param("eventoIds") List<Long> eventoIds);

    // Calcula el monto total bruto generado por las ventas de eventos de una organización en compras confirmadas.
    @Query("""
        SELECT COALESCE(SUM(i.cantidad * i.precioUnitario), 0)
        FROM ItemCompra i
        JOIN i.compra c
        JOIN i.evento e
        WHERE e.organizacion.id = :organizacionId
          AND c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
        """)
    java.math.BigDecimal sumarIngresosPorOrganizacion(@Param("organizacionId") Long organizacionId);

    // Obtiene la cantidad de boletas vendidas en compras confirmadas agrupada por identificador de localidad.
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
