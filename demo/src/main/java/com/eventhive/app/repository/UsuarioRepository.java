package com.eventhive.app.repository;

import com.eventhive.app.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Busca un usuario por correo
    Optional<Usuario> findByCorreo(String correo);

    // Verifica si existe un usuario con ese correo
    boolean existsByCorreo(String correo);

    // Busca usuarios por nombre de rol
    Page<Usuario> findByRolNombre(String nombre, Pageable pageable);

    // Busca un usuario por correo con su rol cargado
    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol WHERE u.correo = :correo")
    Optional<Usuario> findByCorreoConRol(@Param("correo") String correo);

    // Busca usuarios por rol
    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol r WHERE r.id = :rolId")
    Page<Usuario> findByRolId(@Param("rolId") Long rolId, Pageable pageable);

    // Busca usuarios cuyo nombre contiene el texto indicado
    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol WHERE LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    Page<Usuario> findByNombreContieneIgnoreCase(@Param("nombre") String nombre, Pageable pageable);

    // Cuenta usuarios por rol
    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.rol.id = :rolId")
    long countByRolId(@Param("rolId") Long rolId);

    // Busca usuarios por nombre y rol
    @Query("""
        SELECT u FROM Usuario u JOIN FETCH u.rol r
        WHERE LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :nombre, '%'))
        AND r.id = :rolId
        """)
    Page<Usuario> findByNombreYRolId(@Param("nombre") String nombre,
                                     @Param("rolId")   Long rolId,
                                     Pageable pageable);

    // Lista los operadores de una organizacion
    @Query("""
    SELECT u FROM Usuario u
    JOIN FETCH u.rol r
    WHERE u.organizacion.id = :organizacionId
      AND UPPER(r.nombre) = 'OPERADOR'
    ORDER BY u.nombreCompleto ASC
    """)
    Page<Usuario> findOperadoresByOrganizacionId(
            @Param("organizacionId") Long organizacionId,
            Pageable pageable);
}