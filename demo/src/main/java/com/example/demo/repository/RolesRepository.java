package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Rol;

@Repository
public interface RolesRepository extends JpaRepository<Rol, String> {
    Optional<Rol> findByNombre(String nombre);
    boolean existsByNombre(String nombre);
}