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

    @Query("""
        SELECT DISTINCT c FROM Compra c
        JOIN FETCH c.cliente
        LEFT JOIN FETCH c.items i
        LEFT JOIN FETCH i.evento
        LEFT JOIN FETCH i.localidad
        WHERE c.id = :id
        """)
    Optional<Compra> findByIdConItems(@Param("id") Long id);

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

    long countByClienteIdAndEstado(Long clienteId, EstadoCompra estado);

    @Query("SELECT SUM(c.total) FROM Compra c WHERE c.cliente.id = :clienteId AND c.estado = :estado")
    java.math.BigDecimal sumarTotalPorClienteYEstado(@Param("clienteId") Long clienteId,
                                                    @Param("estado") EstadoCompra estado);

    Optional<Compra> findByClienteIdAndIdempotencyKey(Long clienteId, String idempotencyKey);

    @Query("""
        SELECT COALESCE(SUM(c.total), 0) FROM Compra c
        WHERE c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
          AND EXISTS (SELECT 1 FROM ItemCompra i WHERE i.compra = c AND i.evento.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO)
        """)
    java.math.BigDecimal sumarTotalBrutoConfirmado();

    @Query("""
        SELECT COALESCE(SUM(c.netoOrganizador), 0) FROM Compra c
        WHERE c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
          AND EXISTS (SELECT 1 FROM ItemCompra i WHERE i.compra = c AND i.evento.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO)
        """)
    java.math.BigDecimal sumarTotalNetoOrganizadores();

    @Query("""
        SELECT COALESCE(SUM(c.comisionEventhive), 0) FROM Compra c
        WHERE c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
          AND EXISTS (SELECT 1 FROM ItemCompra i WHERE i.compra = c AND i.evento.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO)
        """)
    java.math.BigDecimal sumarComisionesConfirmadas();

    @Query("""
        SELECT COALESCE(SUM(c.comisionEventhive), 0) FROM Compra c
        WHERE c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
          AND c.porcentajeComision > :porcentajeBase
          AND EXISTS (SELECT 1 FROM ItemCompra i WHERE i.compra = c AND i.evento.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO)
        """)
    java.math.BigDecimal sumarComisionesPosicionados(@Param("porcentajeBase") java.math.BigDecimal porcentajeBase);

    @Query("""
        SELECT COALESCE(SUM(c.netoOrganizador), 0) FROM Compra c
        WHERE c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
          AND EXISTS (SELECT 1 FROM ItemCompra i
                      WHERE i.compra = c
                        AND i.evento.organizacion.id = :organizacionId
                        AND i.evento.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO)
        """)
    java.math.BigDecimal sumarNetoPorOrganizacion(@Param("organizacionId") Long organizacionId);

    @Query("""
            SELECT i.evento.id, c.id, c.netoOrganizador
            FROM Compra c JOIN c.items i
            WHERE c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
                AND i.evento.id IN :eventoIds
                AND i.evento.estado = com.eventhive.app.enums.EstadoEvento.PUBLICADO
            GROUP BY i.evento.id, c.id, c.netoOrganizador
            """)
    java.util.List<Object[]> sumarNetoPorEventos(@Param("eventoIds") java.util.List<Long> eventoIds);
}
