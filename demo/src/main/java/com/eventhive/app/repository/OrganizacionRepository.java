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

    // Evita dos organizaciones con el mismo NIT/CC
    boolean existsByNit(String nit);

    @Query("SELECT o FROM Organizacion o LEFT JOIN FETCH o.representante WHERE o.id = :id")
    java.util.Optional<Organizacion> findByIdConRepresentante(@org.springframework.data.repository.query.Param("id") Long id);

    @Query(value = "SELECT o FROM Organizacion o LEFT JOIN FETCH o.representante",
           countQuery = "SELECT COUNT(o) FROM Organizacion o")
    Page<Organizacion> findAllConRepresentante(Pageable pageable);

    // Igual que findAllConRepresentante pero limitado a un estado (p. ej. solo APROBADA)
    @Query(value = "SELECT o FROM Organizacion o LEFT JOIN FETCH o.representante WHERE o.estado = :estado",
           countQuery = "SELECT COUNT(o) FROM Organizacion o WHERE o.estado = :estado")
    Page<Organizacion> findByEstadoConRepresentante(@Param("estado") EstadoOrganizacion estado, Pageable pageable);

    // Top: primero por nivel de confianza, luego por cantidad de seguidores
    @Query(value = "SELECT o FROM Organizacion o LEFT JOIN FETCH o.representante ORDER BY o.nivel DESC, o.totalSeguidores DESC",
           countQuery = "SELECT COUNT(o) FROM Organizacion o")
    Page<Organizacion> findTopOrganizaciones(Pageable pageable);

    @Query(value = "SELECT o FROM Organizacion o LEFT JOIN FETCH o.representante WHERE o.estado = :estado ORDER BY o.nivel DESC, o.totalSeguidores DESC",
           countQuery = "SELECT COUNT(o) FROM Organizacion o WHERE o.estado = :estado")
    Page<Organizacion> findTopByEstado(@Param("estado") EstadoOrganizacion estado, Pageable pageable);

    // Verifica si ya existe una organización con ese correo de contacto.
    boolean existsByCorreoContacto(String correoContacto);

    @Query(value = "SELECT o FROM Organizacion o LEFT JOIN FETCH o.representante WHERE LOWER(o.razonSocial) LIKE LOWER(CONCAT('%', :razonSocial, '%'))",
           countQuery = "SELECT COUNT(o) FROM Organizacion o WHERE LOWER(o.razonSocial) LIKE LOWER(CONCAT('%', :razonSocial, '%'))")
    Page<Organizacion> findByRazonSocial(@org.springframework.data.repository.query.Param("razonSocial") String razonSocial, Pageable pageable);

    @Query(value = "SELECT o FROM Organizacion o LEFT JOIN FETCH o.representante WHERE o.estado = :estado AND LOWER(o.razonSocial) LIKE LOWER(CONCAT('%', :razonSocial, '%'))",
           countQuery = "SELECT COUNT(o) FROM Organizacion o WHERE o.estado = :estado AND LOWER(o.razonSocial) LIKE LOWER(CONCAT('%', :razonSocial, '%'))")
    Page<Organizacion> findByRazonSocialAndEstado(@Param("razonSocial") String razonSocial,
                                                  @Param("estado") EstadoOrganizacion estado,
                                                  Pageable pageable);

    // Cantidad de organizaciones por estado (agregado en BD, sin cargar entidades)
    @Query("SELECT new com.eventhive.app.dto.response.OrganizacionEstadoConteoDTO(o.estado, COUNT(o)) FROM Organizacion o GROUP BY o.estado")
    List<OrganizacionEstadoConteoDTO> contarPorEstado();
}
