package com.eventhive.app.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.Seguidor;
import com.eventhive.app.model.Usuario;

public interface SeguidorRepository extends JpaRepository<Seguidor, Long> {

    // Verifica si un usuario ya sigue a una organización determinada.
    boolean existsByOrganizacionIdAndSeguidorId(Long organizacionId, Long seguidorId);

    // Elimina el registro de seguimiento entre un usuario y una organización.
    void deleteByOrganizacionIdAndSeguidorId(Long organizacionId, Long seguidorId);

    // Obtiene los usuarios que siguen a una organización de forma paginada con su rol precargado.
    @Query(value = "SELECT u FROM Seguidor s JOIN s.seguidor u JOIN FETCH u.rol WHERE s.organizacion.id = :organizacionId",
           countQuery = "SELECT COUNT(s.id) FROM Seguidor s WHERE s.organizacion.id = :organizacionId")
    Page<Usuario> findSeguidoresByOrganizacionId(@Param("organizacionId") Long organizacionId, Pageable pageable);

    // Obtiene la lista completa de usuarios seguidores de una organización.
    @Query("SELECT s.seguidor FROM Seguidor s WHERE s.organizacion.id = :organizacionId")
    List<Usuario> findAllSeguidoresByOrganizacionId(@Param("organizacionId") Long organizacionId);

    // Obtiene las organizaciones que sigue un usuario de forma paginada con su representante precargado.
    @Query(value = "SELECT DISTINCT o FROM Seguidor s JOIN s.organizacion o LEFT JOIN FETCH o.representante WHERE s.seguidor.id = :seguidorId",
           countQuery = "SELECT COUNT(DISTINCT s.organizacion.id) FROM Seguidor s WHERE s.seguidor.id = :seguidorId")
    Page<Organizacion> findOrganizacionesBySeguidorId(@Param("seguidorId") Long seguidorId, Pageable pageable);

    // Cuenta el total de seguidores que posee una organización.
    long countByOrganizacionId(Long organizacionId);
}