package com.eventhive.app.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.eventhive.app.dto.NotificationDTO;
import com.eventhive.app.enums.NivelUsuario;
import com.eventhive.app.enums.TipoNotification;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Notification;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.NotificationRepository;
import com.eventhive.app.repository.SeguidorRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceNotification {

    private final NotificationRepository notificationRepository;
    private final SeguidorRepository     seguidorRepository;
    private final AuthenticatedUserHelper authHelper;

    // 1. Nuevo evento publicado → notificar seguidores del organizador
    public void notificarNuevoEvento(Evento evento) {
        List<Usuario> seguidores = seguidorRepository
                .findSeguidoresByOrganizadorId(evento.getOrganizador().getId());

        seguidores.forEach(seguidor -> crearNotificacion(
                seguidor.getId(),
                evento.getOrganizador().getId(),
                evento.getId(),
                evento.getTitulo(),
                TipoNotification.NUEVO_EVENTO,
                "¡Nuevo evento disponible!",
                evento.getOrganizador().getNombreCompleto()
                        + " publicó: " + evento.getTitulo()
        ));
    }

    // 2. Recordatorio de evento próximo → llamado desde scheduler
    public void notificarRecordatorioEvento(Evento evento) {
        List<Usuario> seguidores = seguidorRepository
                .findSeguidoresByOrganizadorId(evento.getOrganizador().getId());

        seguidores.forEach(seguidor -> crearNotificacion(
                seguidor.getId(),
                evento.getOrganizador().getId(),
                evento.getId(),
                evento.getTitulo(),
                TipoNotification.RECORDATORIO_EVENTO,
                "Recordatorio de evento",
                "El evento \"" + evento.getTitulo() + "\" es mañana. ¡No lo olvides!"
        ));
    }

    // 3. Evento modificado o cancelado → notificar seguidores
    public void notificarCambioEvento(Evento evento, TipoNotification tipo) {
        List<Usuario> seguidores = seguidorRepository
                .findSeguidoresByOrganizadorId(evento.getOrganizador().getId());

        String mensajeBase = tipo == TipoNotification.EVENTO_CANCELADO
                ? "El evento \"" + evento.getTitulo() + "\" ha sido cancelado."
                : "El evento \"" + evento.getTitulo() + "\" fue actualizado.";

        String titulo = tipo == TipoNotification.EVENTO_CANCELADO
                ? "Evento cancelado"
                : "Evento actualizado";

        seguidores.forEach(seguidor -> crearNotificacion(
                seguidor.getId(),
                evento.getOrganizador().getId(),
                evento.getId(),
                evento.getTitulo(),
                tipo,
                titulo,
                mensajeBase
        ));
    }

    // 4. Nuevo nivel de fidelidad → notificar solo al usuario
    public void notificarNuevoNivel(Usuario usuario) {
        NivelUsuario nivel = usuario.getNivel();
        String mensaje = "¡Felicitaciones! Alcanzaste el nivel " + nivel.name()
                + ". Ahora tienes acceso anticipado a eventos.";

        crearNotificacion(
                usuario.getId(),
                null,
                null,
                null,
                TipoNotification.NUEVO_NIVEL_DE_FIDELIDAD,
                "¡Subiste de nivel! 🎉",
                mensaje
        );
    }

    @PreAuthorize("isAuthenticated()")
    public List<NotificationDTO> obtenerMisNotificaciones() {
        Usuario usuario = authHelper.usuarioAutenticado();
        return notificationRepository
                .findByUsuarioIdOrderByFechaCreacionDesc(usuario.getId())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @PreAuthorize("isAuthenticated()")
    public long contarNoLeidas() {
        Usuario usuario = authHelper.usuarioAutenticado();
        return notificationRepository.countByUsuarioIdAndLeidaFalse(usuario.getId());
    }

    @PreAuthorize("isAuthenticated()")
    public void marcarLeida(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setLeida(true);
            notificationRepository.save(n);
        });
    }

    @PreAuthorize("isAuthenticated()")
    public void limpiarLeidas() {
        Usuario usuario = authHelper.usuarioAutenticado();
        notificationRepository.deleteByUsuarioIdAndLeidaTrue(usuario.getId());
    }

    // --- helpers ---
    private void crearNotificacion(Long usuarioId, Long organizadorId, Long eventoId,
                                   String nombreEvento, TipoNotification tipo,
                                   String titulo, String mensaje) {
        Notification n = new Notification();
        n.setUsuarioId(usuarioId);
        n.setOrganizadorId(organizadorId);
        n.setEventoId(eventoId);
        n.setNombreEvento(nombreEvento);
        n.setTipoNotificacion(tipo);
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setLeida(false);
        n.setFechaCreacion(LocalDateTime.now());
        notificationRepository.save(n);
    }

    private NotificationDTO toDTO(Notification n) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(n.getId());
        dto.setTitulo(n.getTitulo());
        dto.setMensaje(n.getMensaje());
        dto.setTipoNotificacion(n.getTipoNotificacion());
        dto.setEventoId(n.getEventoId());
        dto.setNombreEvento(n.getNombreEvento());
        dto.setLeida(n.getLeida());
        dto.setFechaCreacion(n.getFechaCreacion());
        return dto;
    }
}