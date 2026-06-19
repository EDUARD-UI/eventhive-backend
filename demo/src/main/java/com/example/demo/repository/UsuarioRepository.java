package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Usuario findByCorreo(String correo);
    boolean existsByCorreo(String correo);

    // Devuelve usuario por correo incluyendo su rol
    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol WHERE u.correo = :correo")
    Optional<Usuario> findByCorreoConRol(@Param("correo") String correo);

    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol r WHERE r.id = :rolId")
    Page<Usuario> findByRolId(@Param("rolId") Long rolId, Pageable pageable);

    // Busca usuarios por nombre y rol (contains) incluyendo rol
    @Query("""
        SELECT u FROM Usuario u JOIN FETCH u.rol r
        WHERE LOWER(u.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))
        AND r.id = :rolId
        """)
    Page<Usuario> findByNombreYRolId(@Param("nombre") String nombre,
                                      @Param("rolId")   Long rolId,
                                      Pageable pageable);

    // Busca usuarios cuyo nombre contiene el texto (insensible a mayúsculas)
    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol WHERE LOWER(u.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    Page<Usuario> findByNombreContieneIgnoreCase(@Param("nombre") String nombre, Pageable pageable);

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.rol.id = :rolId")
    long countByRolId(@Param("rolId") Long rolId);
}