package com.eventhive.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.model.Tiquete;

public interface TiqueteRepository extends JpaRepository<Tiquete, Long> {

    // Busca un tiquete por su código QR.
    Optional<Tiquete> findByCodigoQR(String codigoQR);

    // Verifica si un evento tiene tiquetes emitidos.
    boolean existsByEventoId(Long eventoId);

    // Verifica si una compra contiene un tiquete usado.
    boolean existsByCompraIdAndUsadoTrue(Long compraId);

    //actualización atómica del check-in. Si dos peticiones llegan a la vez
    @Modifying
    @Query("UPDATE Tiquete t SET t.usado = true WHERE t.codigoQR = :codigoQR AND t.usado = false")
    int marcarComoUsadoSiNoUsado(@Param("codigoQR") String codigoQR);

    // Devuelve los tiquetes de una compra con detalles del evento y la localidad
    @Query("""
        SELECT t FROM Tiquete t
        JOIN FETCH t.evento e
        JOIN FETCH t.localidad l
        JOIN FETCH e.categoria
        WHERE t.compra.id = :compraId
        """)
    List<Tiquete> findByCompraIdConDetalles(@Param("compraId") Long compraId);
}
