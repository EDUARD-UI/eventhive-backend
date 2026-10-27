package com.eventhive.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.model.Localidad;

public interface LocalidadRepository extends JpaRepository<Localidad, Long> {

    // Obtiene todas las localidades configuradas para un evento.
    List<Localidad> findByEventoId(Long eventoId);

    // Cuenta la cantidad de localidades asociadas a un evento.
    long countByEventoId(Long eventoId);

    // Reduce de forma atómica el número de cupos disponibles de una localidad validando existencia suficiente.
    @Modifying
    @Query("UPDATE Localidad l SET l.disponibles = l.disponibles - :cantidad WHERE l.id = :id AND l.disponibles >= :cantidad")
    int decrementarDisponibles(@Param("id") Long id, @Param("cantidad") int cantidad);

    // Incrementa de forma atómica el número de cupos disponibles de una localidad.
    @Modifying
    @Query("UPDATE Localidad l SET l.disponibles = l.disponibles + :cantidad WHERE l.id = :id")
    void incrementarDisponibles(@Param("id") Long id, @Param("cantidad") int cantidad);

    // Obtiene todas las localidades pertenecientes a una lista de identificadores de eventos.
    List<Localidad> findByEventoIdIn(List<Long> eventoIds);

    // Busca una localidad por su identificador precargando el evento y la organización propietaria.
    @Query("SELECT l FROM Localidad l JOIN FETCH l.evento e JOIN FETCH e.organizacion WHERE l.id = :id")
    java.util.Optional<com.eventhive.app.model.Localidad> findByIdConEvento(@Param("id") Long id);
}