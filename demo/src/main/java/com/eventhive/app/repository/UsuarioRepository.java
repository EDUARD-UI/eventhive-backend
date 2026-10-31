package com.eventhive.app.repository;

import com.eventhive.app.enums.EstadoOrganizacion;
import com.eventhive.app.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Busca un usuario por su dirección de correo electrónico exacta.
    Optional<Usuario> findByCorreo(String correo);

    // Busca un usuario por su dirección de correo electrónico ignorando mayúsculas y minúsculas.
    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    // Verifica si existe un usuario registrado con el correo indicado ignorando mayúsculas y minúsculas.
    boolean existsByCorreoIgnoreCase(String correo);

    // Verifica si existe un usuario registrado con la dirección de correo indicada.
    boolean existsByCorreo(String correo);

    // Obtiene usuarios asociados a un rol determinado de forma paginada.
    Page<Usuario> findByRolNombre(String nombre, Pageable pageable);

    // Obtiene usuarios filtrados por rol de forma paginada con el rol precargado ordenados alfabéticamente.
    @Query(value = "SELECT u FROM Usuario u JOIN FETCH u.rol r WHERE r.nombre = :nombre ORDER BY u.nombreCompleto ASC",
           countQuery = "SELECT COUNT(u.id) FROM Usuario u WHERE u.rol.nombre = :nombre")
    Page<Usuario> findByRolNombreConRol(@Param("nombre") String nombre, Pageable pageable);

    // Obtiene la lista completa sin paginar de usuarios asociados a un rol específico.
    List<Usuario> findAllByRolNombre(String nombre);

    // Busca un usuario por su identificador precargando su rol y la organización a la que pertenece.
    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol LEFT JOIN FETCH u.organizacion WHERE u.id = :id")
    Optional<Usuario> findByIdConRol(@Param("id") Long id);

    // Obtiene todos los usuarios del sistema de forma paginada con su rol y organización precargados.
    @Query(value = "SELECT u FROM Usuario u JOIN FETCH u.rol LEFT JOIN FETCH u.organizacion",
           countQuery = "SELECT COUNT(u.id) FROM Usuario u")
    Page<Usuario> findAllConRol(Pageable pageable);

    // Busca un usuario por su correo electrónico precargando su rol.
    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol WHERE u.correo = :correo")
    Optional<Usuario> findByCorreoConRol(@Param("correo") String correo);

    // Busca un usuario por su correo precargando su rol, organización, representante y permisos de eventos.
    @Query("""
        SELECT DISTINCT u FROM Usuario u
        JOIN FETCH u.rol
        LEFT JOIN FETCH u.organizacion o
        LEFT JOIN FETCH o.representante
        LEFT JOIN FETCH u.permisosEvento
        WHERE u.correo = :correo
        """)
    Optional<Usuario> findByCorreoConContexto(@Param("correo") String correo);

    // Obtiene los usuarios asociados a un identificador de rol con referencias precargadas.
    @Query(value = "SELECT u FROM Usuario u JOIN FETCH u.rol r LEFT JOIN FETCH u.organizacion WHERE r.id = :rolId",
           countQuery = "SELECT COUNT(u.id) FROM Usuario u WHERE u.rol.id = :rolId")
    Page<Usuario> findByRolId(@Param("rolId") Long rolId, Pageable pageable);

    // Busca usuarios por coincidencia en nombre o correo ignorando mayúsculas con relaciones precargadas.
    @Query(value = "SELECT u FROM Usuario u JOIN FETCH u.rol LEFT JOIN FETCH u.organizacion WHERE LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :nombre, '%')) OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :nombre, '%'))",
           countQuery = "SELECT COUNT(u.id) FROM Usuario u WHERE LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :nombre, '%')) OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    Page<Usuario> findByNombreContieneIgnoreCase(@Param("nombre") String nombre, Pageable pageable);

    // Cuenta el total de usuarios asignados a un identificador de rol específico.
    @Query("SELECT COUNT(u.id) FROM Usuario u WHERE u.rol.id = :rolId")
    long countByRolId(@Param("rolId") Long rolId);

    // Busca usuarios pertenecientes a un rol cuyo nombre o correo contenga el texto buscado.
    @Query(value = """
        SELECT u FROM Usuario u JOIN FETCH u.rol r LEFT JOIN FETCH u.organizacion
        WHERE (LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :nombre, '%')) OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :nombre, '%')))
        AND r.id = :rolId
        """,
        countQuery = """
        SELECT COUNT(u.id) FROM Usuario u
        WHERE (LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :nombre, '%')) OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :nombre, '%')))
        AND u.rol.id = :rolId
        """)
    Page<Usuario> findByNombreYRolId(@Param("nombre") String nombre,
                                     @Param("rolId")   Long rolId,
                                     Pageable pageable);

    // Obtiene los operadores de una organización de forma paginada con rol precargado ordenados alfabéticamente.
    @Query(value = """
    SELECT u FROM Usuario u
    JOIN FETCH u.rol r
    WHERE u.organizacion.id = :organizacionId
      AND UPPER(r.nombre) = 'OPERADOR'
    ORDER BY u.nombreCompleto ASC
    """,
    countQuery = """
    SELECT COUNT(u.id) FROM Usuario u
    WHERE u.organizacion.id = :organizacionId
      AND UPPER(u.rol.nombre) = 'OPERADOR'
    """)
    Page<Usuario> findOperadoresByOrganizacionId(
            @Param("organizacionId") Long organizacionId,
            Pageable pageable);

    // Consulta filtrada para el panel de administración con carga eficiente de rol y organización.
    @Query(value = """
        SELECT u FROM Usuario u
        JOIN FETCH u.rol r
        LEFT JOIN FETCH u.organizacion o
        WHERE (:nombre IS NULL OR :nombre = '' OR LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :nombre, '%')) OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :nombre, '%')) OR (o IS NOT NULL AND LOWER(o.razonSocial) LIKE LOWER(CONCAT('%', :nombre, '%'))))
          AND (:organizacion IS NULL OR :organizacion = '' OR (o IS NOT NULL AND LOWER(o.razonSocial) LIKE LOWER(CONCAT('%', :organizacion, '%'))))
          AND (:estadoOrg IS NULL OR (o IS NOT NULL AND o.estado = :estadoOrg))
          AND (:activo IS NULL OR u.activo = :activo)
          AND (:rolId IS NULL OR r.id = :rolId)
        """,
        countQuery = """
        SELECT COUNT(u.id) FROM Usuario u
        LEFT JOIN u.organizacion o
        WHERE (:nombre IS NULL OR :nombre = '' OR LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :nombre, '%')) OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :nombre, '%')) OR (o IS NOT NULL AND LOWER(o.razonSocial) LIKE LOWER(CONCAT('%', :nombre, '%'))))
          AND (:organizacion IS NULL OR :organizacion = '' OR (o IS NOT NULL AND LOWER(o.razonSocial) LIKE LOWER(CONCAT('%', :organizacion, '%'))))
          AND (:estadoOrg IS NULL OR (o IS NOT NULL AND o.estado = :estadoOrg))
          AND (:activo IS NULL OR u.activo = :activo)
          AND (:rolId IS NULL OR u.rol.id = :rolId)
        """)
    Page<Usuario> buscarAdminConFiltros(
            @Param("nombre") String nombre,
            @Param("organizacion") String organizacion,
            @Param("estadoOrg") EstadoOrganizacion estadoOrg,
            @Param("activo") Boolean activo,
            @Param("rolId") Long rolId,
            Pageable pageable);
}