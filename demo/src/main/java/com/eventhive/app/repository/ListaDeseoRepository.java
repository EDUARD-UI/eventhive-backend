package com.eventhive.app.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventhive.app.model.Evento;
import com.eventhive.app.model.ListaDeseo;

public interface ListaDeseoRepository extends JpaRepository<ListaDeseo, Long> {

    boolean existsByUsuarioIdAndEventoId(Long usuarioId, Long eventoId);

    void deleteByUsuarioIdAndEventoId(Long usuarioId, Long eventoId);

    @Query("SELECT d.evento FROM ListaDeseo d WHERE d.usuario.id = :usuarioId ORDER BY d.id DESC")
    Page<Evento> findEventosDeseadosByUsuarioId(@Param("usuarioId") Long usuarioId, Pageable pageable);

    long countByEventoId(Long eventoId);
}
