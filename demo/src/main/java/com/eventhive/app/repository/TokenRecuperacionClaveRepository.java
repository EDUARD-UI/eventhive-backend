package com.eventhive.app.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.model.TokenRecuperacionClave;

public interface TokenRecuperacionClaveRepository extends JpaRepository<TokenRecuperacionClave, Long> {

    // Busca un token de recuperación no utilizado a partir de su hash con el usuario precargado.
    @Query("SELECT t FROM TokenRecuperacionClave t JOIN FETCH t.usuario "
            + "WHERE t.tokenHash = :hash AND t.fechaUso IS NULL")
    Optional<TokenRecuperacionClave> findVigentePorHash(@Param("hash") String hash);

    // Registra la fecha de uso de un token de recuperación para evitar su reutilización.
    @Modifying
    @Query("UPDATE TokenRecuperacionClave t SET t.fechaUso = :ahora "
            + "WHERE t.id = :id AND t.fechaUso IS NULL")
    int marcarUsado(@Param("id") Long id, @Param("ahora") LocalDateTime ahora);

    // Invalida todos los tokens de recuperación previos no utilizados de un usuario.
    @Modifying
    @Query("UPDATE TokenRecuperacionClave t SET t.fechaUso = :ahora "
            + "WHERE t.usuario.id = :usuarioId AND t.fechaUso IS NULL")
    int invalidarPendientes(@Param("usuarioId") Long usuarioId, @Param("ahora") LocalDateTime ahora);
}