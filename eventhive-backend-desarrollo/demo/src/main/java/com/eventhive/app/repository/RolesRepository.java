package com.eventhive.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eventhive.app.model.Rol;

public interface RolesRepository extends JpaRepository<Rol, Long> {
    // Busca un rol por su nombre
    Optional<Rol> findByNombre(String nombre);

    // Verifica si existe un rol con ese nombre
    boolean existsByNombre(String nombre);
}