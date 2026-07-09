package com.eventhive.app.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.dto.ModeracionEventoDTO;
import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.enums.MotivosRechazos;
import com.eventhive.app.enums.TipoNotification;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.ModeracionEvento;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.EventoRepository;
import com.eventhive.app.repository.ModeracionEventoRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceModeracion {

    private final EventoRepository eventoRepository;
    private final ModeracionEventoRepository moderacionRepository;
    private final AuthenticatedUserHelper authHelper;
    private final ServiceNotification serviceNotification;

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MODERADOR') or hasRole('ADMINISTRADOR')")
    public Page<Evento> listarPendientesRevision(Pageable pageable) {
        return eventoRepository.findByEstadoConReferencias(EstadoEvento.PENDIENTE_REVISION, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ModeracionEventoDTO> listarModeraciones(Long eventoId, Pageable pageable) {
        Evento evento = obtenerPorId(eventoId);
        Usuario usuario = authHelper.usuarioAutenticado();
        boolean esOwner = evento.getOrganizador() != null && usuario.getId().equals(evento.getOrganizador().getId());
        boolean esStaff = esStaff(usuario);

        if (!esOwner && !esStaff) {
            throw new BusinessException("No autorizado para ver el historial de este evento");
        }

        return moderacionRepository.findByEventoId(eventoId, pageable).map(this::toModeracionDTO);
    }

    @Transactional
    @PreAuthorize("hasRole('MODERADOR') or hasRole('ADMINISTRADOR')")
    public Evento aprobarEvento(Long id) {
        Evento evento = obtenerEnRevision(id);
        evento.setEstado(EstadoEvento.PUBLICADO);
        registrarModeracion(evento, EstadoEvento.PUBLICADO, null, null);

        Evento guardado = eventoRepository.save(evento);
        serviceNotification.notificarNuevoEvento(guardado);
        return guardado;
    }

    @Transactional
    @PreAuthorize("hasRole('MODERADOR') or hasRole('ADMINISTRADOR')")
    public Evento solicitarCorreccion(Long id, MotivosRechazos motivo, String observacion) {
        validarMotivo(motivo, observacion);
        Evento evento = obtenerEnRevision(id);
        evento.setEstado(EstadoEvento.EN_CORRECCION);
        registrarModeracion(evento, EstadoEvento.EN_CORRECCION, motivo, observacion);
        return eventoRepository.save(evento);
    }

    @Transactional
    @PreAuthorize("hasRole('MODERADOR') or hasRole('ADMINISTRADOR')")
    public Evento rechazarEvento(Long id, MotivosRechazos motivo, String observacion) {
        validarMotivo(motivo, observacion);
        Evento evento = obtenerEnRevision(id);
        evento.setEstado(EstadoEvento.RECHAZADO);
        registrarModeracion(evento, EstadoEvento.RECHAZADO, motivo, observacion);

        Usuario organizador = evento.getOrganizador();
        organizador.getOrganizacion().setEventosRechazados(organizador.getOrganizacion().getEventosRechazados() + 1);
        return eventoRepository.save(evento);
    }

    // Única acción del ADMINISTRADOR sobre un evento ya publicado: suspenderlo.
    // No puede crearlo, editarlo ni eliminarlo (eso es exclusivo del organizador dueño).
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Evento suspenderEvento(Long id, MotivosRechazos motivo, String observacion) {
        validarMotivo(motivo, observacion);
        Evento evento = obtenerPorId(id);
        if (evento.getEstado() != EstadoEvento.PUBLICADO)
            throw new BusinessException("Solo se pueden suspender eventos en estado PUBLICADO");

        evento.setEstado(EstadoEvento.SUSPENDIDO);
        registrarModeracion(evento, EstadoEvento.SUSPENDIDO, motivo, observacion);

        Evento guardado = eventoRepository.save(evento);
        serviceNotification.notificarCambioEvento(guardado, TipoNotification.EVENTO_CANCELADO);
        return guardado;
    }

    // Revierte una suspensión: el evento vuelve a estar visible/PUBLICADO
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Evento reactivarEvento(Long id) {
        Evento evento = obtenerPorId(id);
        if (evento.getEstado() != EstadoEvento.SUSPENDIDO)
            throw new BusinessException("Solo se pueden reactivar eventos en estado SUSPENDIDO");

        evento.setEstado(EstadoEvento.PUBLICADO);
        registrarModeracion(evento, EstadoEvento.PUBLICADO, null, null);

        Evento guardado = eventoRepository.save(evento);
        serviceNotification.notificarCambioEvento(guardado, TipoNotification.EVENTO_MODIFICADO);
        return guardado;
    }

    private void validarMotivo(MotivosRechazos motivo, String observacion) {
        if (motivo == null) {
            throw new BusinessException("El motivo es requerido");
        }
        if (motivo == MotivosRechazos.OTRO && (observacion == null || observacion.isBlank())) {
            throw new BusinessException("Debe describir la observación cuando el motivo es OTRO");
        }
    }

    private Evento obtenerEnRevision(Long id) {
        Evento evento = obtenerPorId(id);
        if (evento.getEstado() != EstadoEvento.PENDIENTE_REVISION) {
            throw new BusinessException("Solo se pueden moderar eventos en estado PENDIENTE_REVISION");
        }
        return evento;
    }

    private void registrarModeracion(Evento evento, EstadoEvento resultado, MotivosRechazos motivo, String observacion) {
        ModeracionEvento moderacion = new ModeracionEvento();
        moderacion.setEvento(evento);
        moderacion.setModerador(authHelper.usuarioAutenticado());
        moderacion.setEstadoResultante(resultado);
        moderacion.setMotivo(motivo);
        moderacion.setObservacion(motivo == MotivosRechazos.OTRO ? observacion : null);
        moderacion.setFecha(LocalDateTime.now());
        moderacionRepository.save(moderacion);
    }

    private Evento obtenerPorId(Long id) {
        return eventoRepository.findByIdConReferencias(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id: " + id));
    }

    private boolean esStaff(Usuario usuario) {
        return usuario != null
                && usuario.getRol() != null
                && ("ADMINISTRADOR".equals(usuario.getRol().getNombre())
                    || "MODERADOR".equals(usuario.getRol().getNombre()));
    }

    private ModeracionEventoDTO toModeracionDTO(ModeracionEvento moderacion) {
        ModeracionEventoDTO dto = new ModeracionEventoDTO();
        dto.setId(moderacion.getId());
        dto.setEstadoResultante(moderacion.getEstadoResultante());
        dto.setMotivo(moderacion.getMotivo());
        dto.setObservacion(moderacion.getObservacion());
        dto.setFecha(moderacion.getFecha());
        if (moderacion.getModerador() != null) {
            dto.setModeradorId(moderacion.getModerador().getId());
            dto.setModeradorNombre(moderacion.getModerador().getNombreCompleto());
        }
        return dto;
    }
}
