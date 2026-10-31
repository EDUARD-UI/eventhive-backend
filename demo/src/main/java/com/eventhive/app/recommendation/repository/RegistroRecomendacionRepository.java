package com.eventhive.app.recommendation.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RegistroRecomendacionRepository {

    private final JdbcTemplate jdbcTemplate;

    // Registra el envío una sola vez; permite reclamar nuevamente registros que terminaron en ERROR.
    public boolean reclamarEnvio(Long eventoId, Long usuarioId) {
        String sql = """
            INSERT INTO recomendacion_envios (
                evento_id,
                usuario_id,
                estado,
                fecha_creacion
            )
            VALUES (?, ?, 'PENDIENTE', CURRENT_TIMESTAMP)
            ON CONFLICT (evento_id, usuario_id)
            DO UPDATE SET
                estado = 'PENDIENTE',
                fecha_creacion = CURRENT_TIMESTAMP
            WHERE recomendacion_envios.estado = 'ERROR'
            """;

        try {
            return jdbcTemplate.update(sql, eventoId, usuarioId) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // Marca como enviado después de que el servicio de correo finaliza correctamente.
    public void marcarEnviado(Long eventoId, Long usuarioId) {
        String sql = """
            UPDATE recomendacion_envios
            SET estado = 'ENVIADO',
                fecha_envio = CURRENT_TIMESTAMP
            WHERE evento_id = ?
              AND usuario_id = ?
            """;

        try {
            jdbcTemplate.update(sql, eventoId, usuarioId);
        } catch (Exception ignored) {}
    }

    // Marca el registro como error para permitir un reintento controlado.
    public void marcarError(Long eventoId, Long usuarioId) {
        String sql = """
            UPDATE recomendacion_envios
            SET estado = 'ERROR'
            WHERE evento_id = ?
              AND usuario_id = ?
            """;

        try {
            jdbcTemplate.update(sql, eventoId, usuarioId);
        } catch (Exception ignored) {}
    }
}

