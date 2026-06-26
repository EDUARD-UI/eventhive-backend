package com.eventhive.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.model.Seguidor;
import com.eventhive.app.model.Usuario;

public interface SeguidorRepository extends JpaRepository<Seguidor, Long> {

    boolean existsByOrganizadorIdAndSeguidorId(Long organizadorId, Long seguidorId);

    void deleteByOrganizadorIdAndSeguidorId(Long organizadorId, Long seguidorId);

    // Retorna los seguidores (usuarios) de un organizador
    @Query("SELECT s.seguidor FROM Seguidor s WHERE s.organizador.id = :organizadorId")
    List<Usuario> findSeguidoresByOrganizadorId(@Param("organizadorId") Long organizadorId);

    // Retorna los organizadores que sigue un usuario
    @Query("SELECT s.organizador FROM Seguidor s WHERE s.seguidor.id = :seguidorId")
    List<Usuario> findOrganizadoresBySeguidorId(@Param("seguidorId") Long seguidorId);

    long countByOrganizadorId(Long organizadorId);
}