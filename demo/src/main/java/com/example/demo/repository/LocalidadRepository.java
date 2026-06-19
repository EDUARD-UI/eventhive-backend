package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Localidad;

@Repository
public interface LocalidadRepository extends JpaRepository<Localidad, Long> {

    List<Localidad> findByEventoId(Long eventoId);

    // Decremento atómico para evitar race conditions en compras concurrentes
    @Modifying
    @Query("UPDATE Localidad l SET l.disponibles = l.disponibles - :cantidad WHERE l.id = :id AND l.disponibles >= :cantidad")
    int decrementarDisponibles(@Param("id") Long id, @Param("cantidad") int cantidad);

    @Modifying
    @Query("UPDATE Localidad l SET l.disponibles = l.disponibles + :cantidad WHERE l.id = :id")
    void incrementarDisponibles(@Param("id") Long id, @Param("cantidad") int cantidad);

    // Busca una localidad y trae su evento asociado
    @Query("SELECT l FROM Localidad l JOIN FETCH l.evento WHERE l.id = :id")
    java.util.Optional<com.example.demo.model.Localidad> findByIdConEvento(@Param("id") Long id);
}