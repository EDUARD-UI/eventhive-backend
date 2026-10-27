package com.eventhive.app.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

import org.springframework.data.repository.query.Param;

import com.eventhive.app.dto.response.OrganizacionEstadoConteoDTO;
import com.eventhive.app.enums.EstadoOrganizacion;
import com.eventhive.app.model.Organizacion;

public interface OrganizacionRepository extends JpaRepository<Organizacion, Long> {

    // Verifica si ya existe una organización registrada con el NIT o documento tributario indicado.
    boolean existsByNit(String nit);

    // Busca una organización por su identificador cargando los datos de su representante legal.
    @Query("SELECT o FROM Organizacion o LEFT JOIN FETCH o.representante WHERE o.id = :id")
    java.util.Optional<Organizacion> findByIdConRepresentante(@org.springframework.data.repository.query.Param("id") Long id);

    // Busca una organización a partir del identificador de su representante cargando los datos del mismo.
    @Query("SELECT o FROM Organizacion o LEFT JOIN FETCH o.representante WHERE o.representante.id = :representanteId")
    java.util.Optional<Organizacion> findByRepresentanteIdConRepresentante(@org.springframework.data.repository.query.Param("representanteId") Long representanteId);

    // Busca una organización a partir del identificador de su representante legal.
    java.util.Optional<Organizacion> findByRepresentanteId(Long representanteId);

    // Obtiene todas las organizaciones de forma paginada con su representante legal precargado.
    @Query(value = "SELECT o FROM Organizacion o LEFT JOIN FETCH o.representante",
           countQuery = "SELECT COUNT(o.id) FROM Organizacion o")
    Page<Organizacion> findAllConRepresentante(Pageable pageable);

    // Obtiene las organizaciones filtradas por su estado con su representante legal precargado.
    @Query(value = "SELECT o FROM Organizacion o LEFT JOIN FETCH o.representante WHERE o.estado = :estado",
           countQuery = "SELECT COUNT(o.id) FROM Organizacion o WHERE o.estado = :estado")
    Page<Organizacion> findByEstadoConRepresentante(@Param("estado") EstadoOrganizacion estado, Pageable pageable);

    // Obtiene las organizaciones ordenadas prioritariamente por nivel de confianza y cantidad de seguidores.
    @Query(value = "SELECT o FROM Organizacion o LEFT JOIN FETCH o.representante ORDER BY o.nivel DESC, o.totalSeguidores DESC",
           countQuery = "SELECT COUNT(o.id) FROM Organizacion o")
    Page<Organizacion> findTopOrganizaciones(Pageable pageable);

    // Obtiene las organizaciones con un estado determinado ordenadas por nivel y cantidad de seguidores.
    @Query(value = "SELECT o FROM Organizacion o LEFT JOIN FETCH o.representante WHERE o.estado = :estado ORDER BY o.nivel DESC, o.totalSeguidores DESC",
           countQuery = "SELECT COUNT(o.id) FROM Organizacion o WHERE o.estado = :estado")
    Page<Organizacion> findTopByEstado(@Param("estado") EstadoOrganizacion estado, Pageable pageable);

    // Verifica si existe una organización registrada con el correo de contacto empresarial indicado.
    boolean existsByCorreoContacto(String correoContacto);

    // Busca organizaciones cuyo nombre o razón social contenga el texto indicado con representante precargado.
    @Query(value = "SELECT o FROM Organizacion o LEFT JOIN FETCH o.representante WHERE LOWER(o.razonSocial) LIKE LOWER(CONCAT('%', :razonSocial, '%'))",
           countQuery = "SELECT COUNT(o.id) FROM Organizacion o WHERE LOWER(o.razonSocial) LIKE LOWER(CONCAT('%', :razonSocial, '%'))")
    Page<Organizacion> findByRazonSocial(@org.springframework.data.repository.query.Param("razonSocial") String razonSocial, Pageable pageable);

    // Busca organizaciones con un estado específico cuyo nombre contenga el texto indicado con representante precargado.
    @Query(value = "SELECT o FROM Organizacion o LEFT JOIN FETCH o.representante WHERE o.estado = :estado AND LOWER(o.razonSocial) LIKE LOWER(CONCAT('%', :razonSocial, '%'))",
           countQuery = "SELECT COUNT(o.id) FROM Organizacion o WHERE o.estado = :estado AND LOWER(o.razonSocial) LIKE LOWER(CONCAT('%', :razonSocial, '%'))")
    Page<Organizacion> findByRazonSocialAndEstado(@Param("razonSocial") String razonSocial,
                                                   @Param("estado") EstadoOrganizacion estado,
                                                   Pageable pageable);

    // Obtiene el conteo total de organizaciones agrupado por cada estado registrado en la plataforma.
    @Query("SELECT new com.eventhive.app.dto.response.OrganizacionEstadoConteoDTO(o.estado, COUNT(o.id)) FROM Organizacion o GROUP BY o.estado")
    List<OrganizacionEstadoConteoDTO> contarPorEstado();
}
