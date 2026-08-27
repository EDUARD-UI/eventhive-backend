package com.eventhive.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.model.Tiquete;

public interface TiqueteRepository extends JpaRepository<Tiquete, Long> {

    // Devuelve los tiquetes de una compra con detalles del evento y la localidad
    @Query("""
        SELECT t FROM Tiquete t
        JOIN FETCH t.evento e
        JOIN FETCH t.localidad l
        JOIN FETCH e.categoria
        WHERE t.compra.id = :compraId
        """)
    List<Tiquete> findByCompraIdConDetalles(@Param("compraId") Long compraId);

    Optional<Tiquete> findByCodigoQR(String codigoQR);

    // bug #15: actualización atómica del check-in. Si dos peticiones llegan a la vez,
    // solo una obtiene filasActualizadas == 1; la otra encuentra usado=true y no actualiza nada.
    @Modifying
    @Query("UPDATE Tiquete t SET t.usado = true WHERE t.codigoQR = :codigoQR AND t.usado = false")
    int marcarComoUsadoSiNoUsado(@Param("codigoQR") String codigoQR);

    @Modifying
    void deleteByCompraId(Long compraId);
}
