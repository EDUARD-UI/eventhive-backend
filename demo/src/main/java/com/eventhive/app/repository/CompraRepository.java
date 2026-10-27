package com.eventhive.app.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.enums.EstadoCompra;
import com.eventhive.app.model.Compra;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    // Obtiene las compras de un cliente de forma paginada con el cliente precargado ordenadas por fecha descendente.
    @Query(value = """
        SELECT c FROM Compra c
        JOIN FETCH c.cliente
        WHERE c.cliente.id = :clienteId
        ORDER BY c.fechaCompra DESC
        """,
        countQuery = """
        SELECT COUNT(c.id) FROM Compra c
        WHERE c.cliente.id = :clienteId
        """)
    Page<Compra> findByClienteIdConItems(@Param("clienteId") Long clienteId, Pageable pageable);

    // Busca una compra por su identificador cargando el cliente, los ítems, el evento y la localidad asociados.
    @Query("""
        SELECT DISTINCT c FROM Compra c
        JOIN FETCH c.cliente
        LEFT JOIN FETCH c.items i
        LEFT JOIN FETCH i.evento
        LEFT JOIN FETCH i.localidad
        WHERE c.id = :id
        """)
    Optional<Compra> findByIdConItems(@Param("id") Long id);

    // Actualiza el estado de una compra de forma atómica si pertenece al cliente y se encuentra en un estado cancelable.
    @Modifying
    @Query("""
            UPDATE Compra c
            SET c.estado = :nuevoEstado
            WHERE c.id = :compraId
                AND c.cliente.id = :clienteId
                AND c.estado IN (:estados)
            """)
    int cancelarSiCancelable(@Param("compraId") Long compraId,
                             @Param("clienteId") Long clienteId,
                             @Param("nuevoEstado") EstadoCompra nuevoEstado,
                             @Param("estados") java.util.List<EstadoCompra> estados);

    // Cuenta la cantidad de compras de un cliente que se encuentran en el estado especificado.
    long countByClienteIdAndEstado(Long clienteId, EstadoCompra estado);

    // Calcula la suma total del dinero gastado por un cliente en compras con el estado especificado.
    @Query("SELECT SUM(c.total) FROM Compra c WHERE c.cliente.id = :clienteId AND c.estado = :estado")
    java.math.BigDecimal sumarTotalPorClienteYEstado(@Param("clienteId") Long clienteId,
                                                    @Param("estado") EstadoCompra estado);

    // Busca una compra de un cliente mediante su clave de idempotencia única.
    Optional<Compra> findByClienteIdAndIdempotencyKey(Long clienteId, String idempotencyKey);

    // Calcula la suma total del monto bruto pagado en todas las compras confirmadas.
    @Query("""
        SELECT COALESCE(SUM(c.total), 0) FROM Compra c
        WHERE c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
        """)
    java.math.BigDecimal sumarTotalBrutoConfirmado();

    // Calcula la suma total del monto neto correspondiente a los organizadores por compras confirmadas.
    @Query("""
        SELECT COALESCE(SUM(c.netoOrganizador), 0) FROM Compra c
        WHERE c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
        """)
    java.math.BigDecimal sumarTotalNetoOrganizadores();

    // Calcula la suma total de las comisiones generadas para la plataforma en compras confirmadas.
    @Query("""
        SELECT COALESCE(SUM(c.comisionEventhive), 0) FROM Compra c
        WHERE c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
        """)
    java.math.BigDecimal sumarComisionesConfirmadas();

    // Calcula la suma de comisiones generadas en compras confirmadas de eventos con comisión superior a la base.
    @Query("""
        SELECT COALESCE(SUM(c.comisionEventhive), 0) FROM Compra c
        WHERE c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
          AND c.porcentajeComision > :porcentajeBase
        """)
    java.math.BigDecimal sumarComisionesPosicionados(@Param("porcentajeBase") java.math.BigDecimal porcentajeBase);

    // Calcula el monto neto total generado para una organización específica en compras confirmadas.
    @Query("""
        SELECT COALESCE(SUM(c.netoOrganizador), 0) FROM Compra c
        WHERE c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
          AND EXISTS (SELECT 1 FROM ItemCompra i
                      WHERE i.compra = c AND i.evento.organizacion.id = :organizacionId)
        """)
    java.math.BigDecimal sumarNetoPorOrganizacion(@Param("organizacionId") Long organizacionId);

    // Obtiene el monto neto de organizador agrupado por evento y compra para un listado de eventos confirmados.
    @Query("""
            SELECT i.evento.id, c.id, c.netoOrganizador
            FROM Compra c JOIN c.items i
            WHERE c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
                AND i.evento.id IN :eventoIds
            GROUP BY i.evento.id, c.id, c.netoOrganizador
            """)
    java.util.List<Object[]> sumarNetoPorEventos(@Param("eventoIds") java.util.List<Long> eventoIds);
}
