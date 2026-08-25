package com.eventhive.app.service;

import com.eventhive.app.dto.response.NotificationDTO;
import com.eventhive.app.enums.TipoNotification;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Notification;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.NotificationRepository;
import com.eventhive.app.repository.SeguidorRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceNotification {

    private final NotificationRepository notificationRepository;
    private final SeguidorRepository seguidorRepository;
    private final AuthenticatedUserHelper authHelper;

    //NOTIFICACIONES
    public void notificarNuevoEvento(Evento evento) {
        notificarASeguidores(
                evento,
                TipoNotification.NUEVO_EVENTO,
                "¡Nuevo evento disponible!",
                evento.getOrganizacion().getRazonSocial() + " publicó: " + evento.getTitulo()
        );
    }

    public void notificarRecordatorioEvento(Evento evento) {
        notificarASeguidores(
                evento,
                TipoNotification.RECORDATORIO_EVENTO,
                "Recordatorio de evento",
                "El evento \"" + evento.getTitulo() + "\" es mañana. ¡No lo olvides!"
        );
    }

    public void notificarCambioEvento(Evento evento, TipoNotification tipo) {
        String titulo = tipo == TipoNotification.EVENTO_CANCELADO ? "Evento cancelado" : "Evento actualizado";
        String mensaje = tipo == TipoNotification.EVENTO_CANCELADO
                ? "El evento \"" + evento.getTitulo() + "\" ha sido cancelado."
                : "El evento \"" + evento.getTitulo() + "\" fue actualizado.";

        notificarASeguidores(evento, tipo, titulo, mensaje);
    }

    public void notificarRevocacionRol(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            return;
        }

        Notification n = new Notification();
        n.setUsuarioId(usuario.getId());
        n.setTipoNotificacion(TipoNotification.ROL_REVOCADO);
        n.setTitulo("Rol revocado");
        n.setMensaje("Tu rol de moderador ha sido revocado. Ahora vuelves a tu rol de cliente.");
        n.setLeida(false);
        n.setFechaCreacion(LocalDateTime.now());
        notificationRepository.save(n);
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

    //METODOS AUXILIARES Y MAPEO
    private void notificarASeguidores(Evento evento, TipoNotification tipo,
                                      String titulo, String mensaje) {
        List<Usuario> seguidores = seguidorRepository
                .findAllSeguidoresByOrganizacionId(evento.getOrganizacion().getId());

        seguidores.forEach(seguidor -> crearNotificacion(
                seguidor.getId(),
                evento.getOrganizacion().getId(),
                evento.getId(),
                evento.getTitulo(),
                tipo, titulo, mensaje
        ));
    }

    private void crearNotificacion(Long usuarioId, Long organizacionId, Long eventoId,
                                   String nombreEvento, TipoNotification tipo,
                                   String titulo, String mensaje) {
        Notification n = new Notification();
        n.setUsuarioId(usuarioId);
        n.setOrganizacionId(organizacionId);
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