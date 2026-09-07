package com.eventhive.app.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.model.Evento;
import com.eventhive.app.model.ListaDeseo;

public interface ListaDeseoRepository extends JpaRepository<ListaDeseo, Long> {

    // Verifica si un usuario guardó un evento en su lista de deseos.
    boolean existsByUsuarioIdAndEventoId(Long usuarioId, Long eventoId);

    // Elimina un evento de la lista de deseos de un usuario.
    void deleteByUsuarioIdAndEventoId(Long usuarioId, Long eventoId);

    // Lista los eventos deseados por un usuario, del más reciente al más antiguo.
    @Query("SELECT d.evento FROM ListaDeseo d WHERE d.usuario.id = :usuarioId ORDER BY d.id DESC")
    Page<Evento> findEventosDeseadosByUsuarioId(@Param("usuarioId") Long usuarioId, Pageable pageable);
}
