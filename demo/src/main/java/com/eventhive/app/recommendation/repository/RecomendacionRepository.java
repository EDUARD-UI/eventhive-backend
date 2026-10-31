package com.eventhive.app.recommendation.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.eventhive.app.recommendation.dto.CandidatoRecomendacionDTO;
import com.eventhive.app.recommendation.dto.EventoRecomendacionDTO;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RecomendacionRepository {

    private final JdbcTemplate jdbcTemplate;

    // Consulta el evento publicado y los datos de su categoría.
    public EventoRecomendacionDTO obtenerEventoPublicado(Long eventoId) {
        String sql = """
            SELECT
                e.id,
                e.titulo,
                e.descripcion,
                c.id AS categoria_id,
                c.nombre AS categoria_nombre,
                e.lugar,
                e.fecha
            FROM eventos e
            INNER JOIN categorias c ON c.id = e.categoria_id
            WHERE e.id = ?
              AND e.estado = 'PUBLICADO'
            """;

        List<EventoRecomendacionDTO> resultados = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new EventoRecomendacionDTO(
                rs.getLong("id"),
                rs.getString("titulo"),
                rs.getString("descripcion"),
                rs.getLong("categoria_id"),
                rs.getString("categoria_nombre"),
                rs.getString("lugar"),
                rs.getDate("fecha") != null ? rs.getDate("fecha").toLocalDate() : null
            ),
            eventoId
        );

        return resultados.stream().findFirst().orElse(null);
    }

    // Obtiene usuarios candidatos con favoritos o compras en la categoría del evento.
    public List<CandidatoRecomendacionDTO> obtenerCandidatos(Long eventoId) {
        String sql = """
            WITH evento_objetivo AS (
                SELECT id, categoria_id
                FROM eventos
                WHERE id = ?
                  AND estado = 'PUBLICADO'
            ),
            favoritos AS (
                SELECT
                    ld.usuario_id,
                    COUNT(*) AS favoritos_categoria
                FROM lista_deseos ld
                INNER JOIN eventos e ON e.id = ld.evento_id
                CROSS JOIN evento_objetivo eo
                WHERE e.categoria_id = eo.categoria_id
                GROUP BY ld.usuario_id
            ),
            compras_categoria AS (
                SELECT
                    c.cliente_id AS usuario_id,
                    COUNT(DISTINCT ic.evento_id) AS compras_categoria,
                    SUM(ic.cantidad) AS entradas_compradas
                FROM compras c
                INNER JOIN items_compra ic ON ic.compra_id = c.id
                INNER JOIN eventos e ON e.id = ic.evento_id
                CROSS JOIN evento_objetivo eo
                WHERE c.estado = 'CONFIRMADA'
                  AND e.categoria_id = eo.categoria_id
                GROUP BY c.cliente_id
            ),
            candidatos AS (
                SELECT usuario_id FROM favoritos
                UNION
                SELECT usuario_id FROM compras_categoria
            )
            SELECT
                u.id AS usuario_id,
                u.nombre_completo AS nombre_completo,
                u.correo AS correo,
                COALESCE(f.favoritos_categoria, 0) AS favoritos_categoria,
                COALESCE(cc.compras_categoria, 0) AS compras_categoria,
                COALESCE(cc.entradas_compradas, 0) AS entradas_compradas
            FROM candidatos ca
            INNER JOIN usuarios u ON u.id = ca.usuario_id
            LEFT JOIN favoritos f ON f.usuario_id = u.id
            LEFT JOIN compras_categoria cc ON cc.usuario_id = u.id
            CROSS JOIN evento_objetivo eo
            WHERE u.activo = TRUE
              AND NOT EXISTS (
                  SELECT 1
                  FROM compras compra_actual
                  INNER JOIN items_compra item_actual
                      ON item_actual.compra_id = compra_actual.id
                  WHERE compra_actual.cliente_id = u.id
                    AND compra_actual.estado = 'CONFIRMADA'
                    AND item_actual.evento_id = eo.id
              )
            ORDER BY (
                COALESCE(f.favoritos_categoria, 0) * 4 +
                COALESCE(cc.compras_categoria, 0) * 5
            ) DESC
            LIMIT 200
            """;

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new CandidatoRecomendacionDTO(
                rs.getLong("usuario_id"),
                rs.getString("nombre_completo"),
                rs.getString("correo"),
                rs.getLong("favoritos_categoria"),
                rs.getLong("compras_categoria"),
                rs.getLong("entradas_compradas")
            ),
            eventoId
        );
    }

    // Obtiene métricas de comportamiento de un usuario específico para la categoría de un evento.
    public CandidatoRecomendacionDTO obtenerMetricasUsuarioParaEvento(Long usuarioId, Long eventoId) {
        String sql = """
            WITH evento_objetivo AS (
                SELECT id, categoria_id
                FROM eventos
                WHERE id = ?
            ),
            favoritos AS (
                SELECT COUNT(*) AS fav_count
                FROM lista_deseos ld
                INNER JOIN eventos e ON e.id = ld.evento_id
                CROSS JOIN evento_objetivo eo
                WHERE ld.usuario_id = ?
                  AND e.categoria_id = eo.categoria_id
            ),
            compras_cat AS (
                SELECT
                    COUNT(DISTINCT ic.evento_id) AS comp_count,
                    COALESCE(SUM(ic.cantidad), 0) AS ent_count
                FROM compras c
                INNER JOIN items_compra ic ON ic.compra_id = c.id
                INNER JOIN eventos e ON e.id = ic.evento_id
                CROSS JOIN evento_objetivo eo
                WHERE c.cliente_id = ?
                  AND c.estado = 'CONFIRMADA'
                  AND e.categoria_id = eo.categoria_id
            )
            SELECT
                ? AS usuario_id,
                u.nombre_completo,
                u.correo,
                f.fav_count AS favoritos_categoria,
                cc.comp_count AS compras_categoria,
                cc.ent_count AS entradas_compradas
            FROM usuarios u
            CROSS JOIN favoritos f
            CROSS JOIN compras_cat cc
            WHERE u.id = ?
            """;

        List<CandidatoRecomendacionDTO> list = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new CandidatoRecomendacionDTO(
                rs.getLong("usuario_id"),
                rs.getString("nombre_completo"),
                rs.getString("correo"),
                rs.getLong("favoritos_categoria"),
                rs.getLong("compras_categoria"),
                rs.getLong("entradas_compradas")
            ),
            eventoId, usuarioId, usuarioId, usuarioId, usuarioId
        );

        return list.stream().findFirst().orElse(new CandidatoRecomendacionDTO(usuarioId, "", "", 0L, 0L, 0L));
    }
}

