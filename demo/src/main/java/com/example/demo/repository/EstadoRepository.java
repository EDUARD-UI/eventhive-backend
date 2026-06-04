package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Estado;

@Repository
public interface EstadoRepository extends JpaRepository<Estado, String> {
    Optional<Estado> findByNombre(String nombre);
    boolean existsByNombre(String nombre);
}