package com.eventhive.app.repository;

import java.util.List;
import java.util.Optional;

import com.eventhive.app.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.model.Tiquete;

public interface TiqueteRepository extends JpaRepository<Tiquete, Long> {

    // Busca un tiquete mediante su código QR único.
    Optional<Tiquete> findByCodigoQR(String codigoQR);

    // Verifica si existen tiquetes emitidos para un evento específico.
    boolean existsByEventoId(Long eventoId);

    // Verifica si una compra cuenta con al menos un tiquete que ya ha sido utilizado en taquilla.
    boolean existsByCompraIdAndUsadoTrue(Long compraId);

    // Realiza la validación y check-in atómico marcando un tiquete como usado si aún no lo estaba.
    @Modifying
    @Query("UPDATE Tiquete t SET t.usado = true WHERE t.codigoQR = :codigoQR AND t.usado = false")
    int marcarComoUsadoSiNoUsado(@Param("codigoQR") String codigoQR);

    // Obtiene los tiquetes asociados a una compra con detalles del evento, localidad y categoría precargados.
    @Query("""
        SELECT t FROM Tiquete t
        JOIN FETCH t.evento e
        JOIN FETCH t.localidad l
        JOIN FETCH e.categoria
        WHERE t.compra.id = :compraId
        """)
    List<Tiquete> findByCompraIdConDetalles(@Param("compraId") Long compraId);

    // Verifica si un cliente posee al menos un tiquete de una compra confirmada para eventos de una organización.
    @Query("""
    SELECT COUNT(t) > 0
    FROM Tiquete t
    JOIN t.compra c
    JOIN t.evento e
    WHERE c.cliente.id = :clienteId
      AND c.estado = com.eventhive.app.enums.EstadoCompra.CONFIRMADA
      AND e.organizacion.id = :organizacionId
    """)
    boolean existsCompraConfirmadaPorClienteYOrganizacion(
            @Param("clienteId") Long clienteId,
            @Param("organizacionId") Long organizacionId);

    // Obtiene la lista de usuarios compradores distintos que adquirieron boletas para un evento.
    @Query("SELECT DISTINCT c.cliente FROM Tiquete t JOIN t.compra c WHERE t.evento.id = :eventoId")
    List<Usuario> findClientesDistinctByEventoId(@Param("eventoId") Long eventoId);
}
