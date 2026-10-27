package com.eventhive.app.repository;

import java.util.List;

import com.eventhive.app.enums.TipoNotification;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.eventhive.app.model.Notification;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    // Obtiene las últimas cincuenta notificaciones de un usuario ordenadas por fecha de creación descendente.
    List<Notification> findTop50ByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    // Obtiene todas las notificaciones de un usuario ordenadas por fecha de creación descendente.
    List<Notification> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    // Cuenta las notificaciones que aún no han sido leídas por el usuario.
    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    // Elimina todas las notificaciones marcadas como leídas para un usuario.
    void deleteByUsuarioIdAndLeidaTrue(Long usuarioId);

    // Verifica si ya existe una notificación registrada para un usuario, evento y tipo específico.
    boolean existsByUsuarioIdAndEventoIdAndTipoNotificacion(
            Long usuarioId,
            Long eventoId,
            TipoNotification tipoNotificacion);
}