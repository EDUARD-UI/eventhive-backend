package com.eventhive.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eventhive.app.model.Rol;

public interface RolesRepository extends JpaRepository<Rol, Long> {

    // Busca un rol por su nombre único en el sistema.
    Optional<Rol> findByNombre(String nombre);

    // Verifica si ya existe un rol registrado con el nombre especificado.
    boolean existsByNombre(String nombre);
}