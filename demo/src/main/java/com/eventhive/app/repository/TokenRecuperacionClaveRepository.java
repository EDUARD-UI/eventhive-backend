package com.eventhive.app.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.model.TokenRecuperacionClave;

public interface TokenRecuperacionClaveRepository extends JpaRepository<TokenRecuperacionClave, Long> {

    @Query("SELECT t FROM TokenRecuperacionClave t JOIN FETCH t.usuario "
            + "WHERE t.tokenHash = :hash AND t.fechaUso IS NULL")
    Optional<TokenRecuperacionClave> findVigentePorHash(@Param("hash") String hash);

    @Modifying
    @Query("UPDATE TokenRecuperacionClave t SET t.fechaUso = :ahora "
            + "WHERE t.id = :id AND t.fechaUso IS NULL")
    int marcarUsado(@Param("id") Long id, @Param("ahora") LocalDateTime ahora);

    @Modifying
    @Query("UPDATE TokenRecuperacionClave t SET t.fechaUso = :ahora "
            + "WHERE t.usuario.id = :usuarioId AND t.fechaUso IS NULL")
    int invalidarPendientes(@Param("usuarioId") Long usuarioId, @Param("ahora") LocalDateTime ahora);
}