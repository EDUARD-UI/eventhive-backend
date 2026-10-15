package com.eventhive.app.repository;

import java.util.List;

import com.eventhive.app.enums.TipoNotification;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.eventhive.app.model.Notification;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    // Devuelve las notificaciones de un usuario ordenadas por fecha (máx 50 para proteger memoria)
    List<Notification> findTop50ByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    List<Notification> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    // Cuenta las notificaciones no leídas de un usuario
    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    // Elimina las notificaciones leídas de un usuario
    void deleteByUsuarioIdAndLeidaTrue(Long usuarioId);

    boolean existsByUsuarioIdAndEventoIdAndTipoNotificacion(
            Long usuarioId,
            Long eventoId,
            TipoNotification tipoNotificacion);
}