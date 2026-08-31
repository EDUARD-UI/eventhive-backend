package com.eventhive.app.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.dto.response.ModeracionEventoDTO;
import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.enums.MotivosRechazos;
import com.eventhive.app.enums.TipoNotification;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.ModeracionEvento;
import com.eventhive.app.model.Organizacion;
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
    private final ServiceEvento serviceEvento;

    //CONSULTAS
    @Transactional(readOnly = true)
    public Page<Evento> listarPendientesRevision(Pageable pageable) {
        return eventoRepository.findByEstadoConReferencias(EstadoEvento.PENDIENTE_REVISION, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ModeracionEventoDTO> HistorialModeraciones(Long eventoId, Pageable pageable) {
        Evento evento = obtenerPorId(eventoId);//validar que el evento existe
        Usuario usuario = authHelper.usuarioAutenticado();

        Organizacion organizacion = evento.getOrganizacion();
        boolean esRepresentante = organizacion != null
                && organizacion.getRepresentante() != null
                && usuario.getId().equals(organizacion.getRepresentante().getId());

        if (!esRepresentante) {
            throw new BusinessException("No autorizado para ver el historial de este evento");
        }

        return moderacionRepository.findByEventoId(eventoId, pageable).map(this::toModeracionDTO);
    }

    //OPERACIONES DE MODERACION
    @Transactional
    public Evento aprobarEvento(Long eventoId) {
        Evento evento = obtenerEnRevision(eventoId);
        serviceEvento.transicionarEstado(evento, EstadoEvento.PUBLICADO);
        registrarModeracion(evento, EstadoEvento.PUBLICADO, null, null);

        Evento guardado = eventoRepository.save(evento);
        serviceNotification.notificarNuevoEvento(guardado);
        return guardado;
    }

    @Transactional
    public Evento solicitarCorreccion(Long eventoId, MotivosRechazos motivo, String observacion) {
        validarMotivo(motivo, observacion);
        Evento evento = obtenerEnRevision(eventoId);
        serviceEvento.transicionarEstado(evento, EstadoEvento.EN_CORRECCION);
        registrarModeracion(evento, EstadoEvento.EN_CORRECCION, motivo, observacion);
        return eventoRepository.save(evento);
    }

    @Transactional
    public Evento rechazarEvento(Long eventoId, MotivosRechazos motivo, String observacion) {
        validarMotivo(motivo, observacion);
        Evento evento = obtenerEnRevision(eventoId);
        serviceEvento.transicionarEstado(evento, EstadoEvento.RECHAZADO);
        registrarModeracion(evento, EstadoEvento.RECHAZADO, motivo, observacion);

        Organizacion organizacion = evento.getOrganizacion();
        organizacion.setEventosRechazados(organizacion.getEventosRechazados() + 1);
        return eventoRepository.save(evento);
    }

    @Transactional
    public Evento suspenderEvento(Long eventoId, MotivosRechazos motivo, String observacion) {
        validarMotivo(motivo, observacion);
        Evento evento = obtenerPorId(eventoId);
        if (evento.getEstado() != EstadoEvento.PUBLICADO)
            throw new BusinessException("Solo se pueden suspender eventos en estado PUBLICADO");

        serviceEvento.transicionarEstado(evento, EstadoEvento.SUSPENDIDO);
        registrarModeracion(evento, EstadoEvento.SUSPENDIDO, motivo, observacion);

        Evento guardado = eventoRepository.save(evento);
        serviceNotification.notificarCambioEvento(guardado, TipoNotification.EVENTO_CANCELADO);
        return guardado;
    }

    // Revierte una suspensión: el evento vuelve a estar visible/PUBLICADO
    @Transactional
    public Evento reactivarEvento(Long eventoId) {
        Evento evento = obtenerPorId(eventoId);
        if (evento.getEstado() != EstadoEvento.SUSPENDIDO)
            throw new BusinessException("Solo se pueden reactivar eventos en estado SUSPENDIDO");

        serviceEvento.transicionarEstado(evento, EstadoEvento.PUBLICADO);
        registrarModeracion(evento, EstadoEvento.PUBLICADO, null, null);

        Evento guardado = eventoRepository.save(evento);
        serviceNotification.notificarCambioEvento(guardado, TipoNotification.EVENTO_MODIFICADO);
        return guardado;
    }

    //METODOS AUXILIARES
    //validar que exista un motivo y la observacion si es OTRO
    private void validarMotivo(MotivosRechazos motivo, String observacion) {
        if (motivo == null) {
            throw new BusinessException("El motivo es requerido");
        }
        if (motivo == MotivosRechazos.OTRO && (observacion == null || observacion.isBlank())) {
            throw new BusinessException("Debe describir la observación cuando el motivo es OTRO");
        }
    }

    //validar que el evento este en revision
    private Evento obtenerEnRevision(Long eventoId) {
        Evento evento = obtenerPorId(eventoId);
        if (evento.getEstado() != EstadoEvento.PENDIENTE_REVISION) {
            throw new BusinessException("Solo se pueden moderar eventos en estado PENDIENTE_REVISION");
        }
        return evento;
    }

    private void registrarModeracion(Evento evento, EstadoEvento estadoFinal, MotivosRechazos motivo, String observacion) {
        ModeracionEvento moderacion = new ModeracionEvento();
        moderacion.setEvento(evento);
        moderacion.setModerador(authHelper.usuarioAutenticado());
        moderacion.setEstadoResultante(estadoFinal);
        moderacion.setMotivo(motivo);
        moderacion.setObservacion(motivo == MotivosRechazos.OTRO ? observacion : null);
        moderacion.setFecha(LocalDateTime.now());
        moderacionRepository.save(moderacion);
    }

    private Evento obtenerPorId(Long eventoId) {
        return eventoRepository.findByIdConReferencias(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id: " + eventoId));
    }

    //METODOS DE MAPEO
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
