package com.eventhive.app.service;

import com.eventhive.app.dto.response.NotificationDTO;
import com.eventhive.app.enums.TipoNotification;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Compra;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Notification;
import com.eventhive.app.model.Tiquete;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.NotificationRepository;
import com.eventhive.app.repository.SeguidorRepository;
import com.eventhive.app.repository.TiqueteRepository;
import com.eventhive.app.repository.UsuarioRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceNotification {

    private final NotificationRepository notificationRepository;
    private final SeguidorRepository seguidorRepository;
    private final TiqueteRepository tiqueteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServiceCorreo serviceCorreo;
    private final AuthenticatedUserHelper authHelper;

    // NOTIFICACIONES DE EVENTOS

    public void notificarNuevoEvento(Evento evento) {
        notificarASeguidores(
                evento, TipoNotification.NUEVO_EVENTO,
                "¡Nuevo evento disponible!",
                evento.getOrganizacion().getRazonSocial() + " publicó: " + evento.getTitulo());
    }

    // Solo a quienes compraron boleto (no a todos los seguidores)
    public void notificarRecordatorioEvento(Evento evento) {
        List<Usuario> compradores = tiqueteRepository.findClientesDistinctByEventoId(evento.getId());

        for (Usuario comprador : compradores) {
            guardarNotificacion(comprador.getId(), evento.getOrganizacion().getId(), evento.getId(),
                    evento.getTitulo(), TipoNotification.RECORDATORIO_EVENTO,
                    "Recordatorio de evento",
                    "El evento \"" + evento.getTitulo() + "\" es mañana. ¡No lo olvides!");
            serviceCorreo.enviarRecordatorioEventoHoy(evento, comprador);
        }
    }

    // EVENTO_CANCELADO: solo compradores + correo. EVENTO_MODIFICADO: seguidores, sin correo.
    public void notificarCambioEvento(Evento evento, TipoNotification tipo) {
        if (tipo == TipoNotification.EVENTO_CANCELADO) {
            List<Usuario> compradores = tiqueteRepository.findClientesDistinctByEventoId(evento.getId());
            for (Usuario comprador : compradores) {
                guardarNotificacion(comprador.getId(), evento.getOrganizacion().getId(), evento.getId(),
                        evento.getTitulo(), tipo, "Evento cancelado",
                        "El evento \"" + evento.getTitulo() + "\" ha sido cancelado.");
                serviceCorreo.enviarEventoCancelado(evento, comprador);
            }
            return;
        }

        notificarASeguidores(evento, tipo, "Evento actualizado",
                "El evento \"" + evento.getTitulo() + "\" fue actualizado.");
    }

    public void notificarEventoEnviadoRevision(Evento evento) {
        List<Usuario> moderadores = usuarioRepository.findAllByRolNombre("MODERADOR");
        for (Usuario moderador : moderadores) {
            guardarNotificacion(moderador.getId(), evento.getOrganizacion().getId(), evento.getId(),
                    evento.getTitulo(), TipoNotification.EVENTO_ENVIADO_REVISION,
                    "Evento pendiente de revisión",
                    "El evento \"" + evento.getTitulo() + "\" fue enviado a revisión.");
        }
        // Sin correo: la bandeja de moderación ya es el canal principal para ellos.
    }

    public void notificarResultadoModeracionEvento(Usuario representante, Evento evento,
                                                   boolean aprobado, String motivo) {
        TipoNotification tipo = aprobado ? TipoNotification.EVENTO_APROBADO : TipoNotification.EVENTO_RECHAZADO;
        String titulo = aprobado ? "Evento aprobado" : "Evento rechazado";
        String mensaje = aprobado
                ? "Tu evento \"" + evento.getTitulo() + "\" ha sido aprobado."
                : "Tu evento \"" + evento.getTitulo() + "\" ha sido rechazado."
                + (motivo != null ? " Motivo: " + motivo : "");

        guardarNotificacion(representante.getId(), evento.getOrganizacion().getId(), evento.getId(),
                evento.getTitulo(), tipo, titulo, mensaje);
        serviceCorreo.enviarResultadoModeracionEvento(
                representante.getCorreo(), representante.getNombreCompleto(),
                evento.getTitulo(), aprobado, motivo);
    }

    // NOTIFICACIONES DE COMPRAS

    public void notificarCompraConfirmada(Compra compra) {
        Usuario cliente = compra.getCliente();
        guardarNotificacion(cliente.getId(), null, null, null,
                TipoNotification.COMPRA_CONFIRMADA, "Compra confirmada",
                "Tu compra #" + compra.getId() + " fue procesada correctamente. Ya puedes consultar tus boletos.");
        serviceCorreo.enviarConfirmacionCompra(compra);
    }

    public void notificarCompraCancelada(Compra compra) {
        Usuario cliente = compra.getCliente();
        guardarNotificacion(cliente.getId(), null, null, null,
                TipoNotification.COMPRA_CANCELADA, "Compra cancelada",
                "Tu compra #" + compra.getId() + " fue cancelada.");
        serviceCorreo.enviarCancelacionCompra(compra);
    }

    // NOTIFICACIONES DE ORGANIZACIÓN Y ROLES

    public void notificarInvitacionOrganizacion(Usuario invitado, String organizacionNombre) {
        guardarNotificacion(invitado.getId(), null, null, null,
                TipoNotification.ORGANIZACION_INVITACION, "Invitación recibida",
                "Has recibido una invitación para formar parte de " + organizacionNombre + ".");
        serviceCorreo.enviarInvitacionOrganizacion(
                invitado.getCorreo(), invitado.getNombreCompleto(), organizacionNombre);
    }

    public void notificarInvitacionAceptada(Usuario representante, String nombreAceptante) {
        guardarNotificacion(representante.getId(), null, null, null,
                TipoNotification.ORGANIZACION_INVITACION_ACEPTADA, "Invitación aceptada",
                nombreAceptante + " aceptó la invitación para formar parte de tu organización.");
        // Sin correo, según matriz.
    }

    public void notificarInvitacionRechazada(Usuario representante, String nombreRechazante) {
        guardarNotificacion(representante.getId(), null, null, null,
                TipoNotification.ORGANIZACION_INVITACION_RECHAZADA, "Invitación rechazada",
                nombreRechazante + " rechazó la invitación.");
        // Sin correo, según matriz.
    }

    public void notificarRolAsignado(Usuario usuario, String rolNombre, String organizacionNombre) {
        guardarNotificacion(usuario.getId(), null, null, null,
                TipoNotification.ROL_ASIGNADO, "Rol asignado",
                "Has aceptado la invitación para formar parte de " + organizacionNombre
                        + ". Tu rol ahora es " + rolNombre + ".");
        // Sin correo, según matriz.
    }

    // Revocación de rol de MODERADOR (vuelve a CLIENTE)
    public void notificarRevocacionRol(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) return;

        guardarNotificacion(usuario.getId(), null, null, null,
                TipoNotification.ROL_REVOCADO, "Rol revocado",
                "Tu rol de moderador ha sido revocado. Ahora vuelves a tu rol de cliente.");
        serviceCorreo.enviarRolRevocado(usuario.getCorreo(), usuario.getNombreCompleto(),
                "Tu rol de moderador ha sido revocado. Ahora vuelves a tu rol de cliente.");
    }

    // Revocación de permisos de OPERADOR dentro de una organización
    public void notificarRevocacionOperador(Usuario operador, String organizacionNombre) {
        guardarNotificacion(operador.getId(), null, null, null,
                TipoNotification.ROL_REVOCADO, "Rol revocado",
                "Tu rol de OPERADOR en " + organizacionNombre + " ha sido revocado.");
        serviceCorreo.enviarRolRevocado(operador.getCorreo(), operador.getNombreCompleto(),
                "Tu rol de OPERADOR en " + organizacionNombre + " ha sido revocado.");
    }

    public void notificarSolicitudOrganizacion(Usuario representante, boolean aprobada, String motivo) {
        TipoNotification tipo = aprobada
                ? TipoNotification.SOLICITUD_ORGANIZACION_APROBADA
                : TipoNotification.SOLICITUD_ORGANIZACION_RECHAZADA;
        String titulo = aprobada ? "Solicitud aprobada" : "Solicitud rechazada";
        String mensaje = aprobada
                ? "Tu solicitud de organización fue aprobada."
                : "Tu solicitud de organización fue rechazada." + (motivo != null ? " Motivo: " + motivo : "");

        guardarNotificacion(representante.getId(), null, null, null, tipo, titulo, mensaje);
        serviceCorreo.enviarResultadoSolicitudOrganizacion(
                representante.getCorreo(), representante.getNombreCompleto(), aprobada, motivo);
    }

    // OPERACIONES DE NOTIFICACIONES
    public List<NotificationDTO> obtenerMisNotificaciones() {
        Usuario usuario = authHelper.usuarioAutenticado();
        return notificationRepository
                .findByUsuarioIdOrderByFechaCreacionDesc(usuario.getId())
                .stream().map(this::toDTO).toList();
    }

    public long contarNoLeidas() {
        Usuario usuario = authHelper.usuarioAutenticado();
        return notificationRepository.countByUsuarioIdAndLeidaFalse(usuario.getId());
    }

    public void marcarLeida(String notificationId) {
        Usuario usuario = authHelper.usuarioAutenticado();
        Notification notificacion = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));

        if (!notificacion.getUsuarioId().equals(usuario.getId())) {
            throw new ResourceNotFoundException("Notificación no encontrada");
        }
        notificacion.setLeida(true);
        notificationRepository.save(notificacion);
    }

    public void limpiarLeidas() {
        Usuario usuario = authHelper.usuarioAutenticado();
        notificationRepository.deleteByUsuarioIdAndLeidaTrue(usuario.getId());
    }

    //METODOS AUXILIARES Y MAPEO
    private void notificarASeguidores(Evento evento, TipoNotification tipo, String titulo, String mensaje) {
        List<Usuario> seguidores = seguidorRepository
                .findAllSeguidoresByOrganizacionId(evento.getOrganizacion().getId());

        List<Notification> notificaciones = seguidores.stream()
                .map(seguidor -> crearNotificacion(seguidor.getId(), evento.getOrganizacion().getId(),
                        evento.getId(), evento.getTitulo(), tipo, titulo, mensaje))
                .toList();

        notificationRepository.saveAll(notificaciones);
    }

    private void guardarNotificacion(Long usuarioId, Long organizacionId, Long eventoId,
                                     String nombreEvento, TipoNotification tipo, String titulo, String mensaje) {
        notificationRepository.save(
                crearNotificacion(usuarioId, organizacionId, eventoId, nombreEvento, tipo, titulo, mensaje));
    }

    private Notification crearNotificacion(Long usuarioId, Long organizacionId, Long eventoId,
                                           String nombreEvento, TipoNotification tipo, String titulo, String mensaje) {
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
        return n;
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