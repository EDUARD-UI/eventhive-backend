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

    // Obtiene compras de un cliente con sus ítems y detalles cargados
    @Query(value = """
        SELECT DISTINCT c FROM Compra c
        JOIN FETCH c.cliente
        LEFT JOIN FETCH c.items i
        LEFT JOIN FETCH i.evento
        LEFT JOIN FETCH i.localidad
        WHERE c.cliente.id = :clienteId
        ORDER BY c.fechaCompra DESC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT c) FROM Compra c
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

    // Busca una compra del cliente por su clave de idempotencia.
    Optional<Compra> findByClienteIdAndIdempotencyKey(Long clienteId, String idempotencyKey);
}