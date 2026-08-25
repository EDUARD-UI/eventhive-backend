package com.eventhive.app.repository;

import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.Seguidor;
import com.eventhive.app.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeguidorRepository extends JpaRepository<Seguidor, Long> {

    //validar si existe la relacion de seguidor
    boolean existsByOrganizacionIdAndSeguidorId(Long organizacionId, Long seguidorId);

    //elimnar la relacion de seguidor
    void deleteByOrganizacionIdAndSeguidorId(Long organizacionId, Long seguidorId);

    // Retorna paginados los seguidores de una organización
    @Query("SELECT s.seguidor FROM Seguidor s WHERE s.organizacion.id = :organizacionId")
    Page<Usuario> findSeguidoresByOrganizacionId(@Param("organizacionId") Long organizacionId, Pageable pageable);

    // Retorna todos los seguidores de una organización sin paginacion
    @Query(" SELECT s.seguidor FROM Seguidor s WHERE s.organizacion.id = :organizacionId")
    List<Usuario> findAllSeguidoresByOrganizacionId(@Param("organizacionId") Long organizacionId);

    // Retorna paginadas las organizaciones que sigue un usuario
    @Query("SELECT s.organizacion FROM Seguidor s WHERE s.seguidor.id = :seguidorId")
    Page<Organizacion> findOrganizacionesBySeguidorId(@Param("seguidorId") Long seguidorId, Pageable pageable);

    //numero total de seguidores de una organizacion
    long countByOrganizacionId(Long organizacionId);
}