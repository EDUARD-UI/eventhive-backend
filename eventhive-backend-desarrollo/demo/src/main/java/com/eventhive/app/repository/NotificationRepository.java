package com.eventhive.app.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.eventhive.app.model.Notification;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    // Devuelve las notificaciones de un usuario ordenadas por fecha
    List<Notification> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    // Cuenta las notificaciones no leídas de un usuario
    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    // Elimina las notificaciones leídas de un usuario
    void deleteByUsuarioIdAndLeidaTrue(Long usuarioId);
}