package com.eventhive.app.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.eventhive.app.model.Notification;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    void deleteByUsuarioIdAndLeidaTrue(Long usuarioId);
}