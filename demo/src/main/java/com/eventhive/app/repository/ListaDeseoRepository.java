package com.eventhive.app.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.model.Evento;
import com.eventhive.app.model.ListaDeseo;

public interface ListaDeseoRepository extends JpaRepository<ListaDeseo, Long> {

    // Cuenta el número de eventos guardados en la lista de deseos de un usuario.
    long countByUsuarioId(Long usuarioId);

    // Verifica si un usuario guardó un evento en su lista de deseos.
    boolean existsByUsuarioIdAndEventoId(Long usuarioId, Long eventoId);

    // Elimina un evento de la lista de deseos de un usuario.
    void deleteByUsuarioIdAndEventoId(Long usuarioId, Long eventoId);

    // Obtiene los eventos guardados en la lista de deseos de un usuario con categoría y organización precargadas.
    @Query(value = """
        SELECT e FROM ListaDeseo d
        JOIN d.evento e
        JOIN FETCH e.categoria
        JOIN FETCH e.organizacion
        WHERE d.usuario.id = :usuarioId
        ORDER BY d.id DESC
        """,
        countQuery = """
        SELECT COUNT(d.id) FROM ListaDeseo d
        WHERE d.usuario.id = :usuarioId
        """)
    Page<Evento> findEventosDeseadosByUsuarioId(@Param("usuarioId") Long usuarioId, Pageable pageable);
}
