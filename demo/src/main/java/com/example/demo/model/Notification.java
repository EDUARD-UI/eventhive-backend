package com.example.demo.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;              // ✅ CORRECTO para MongoDB
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.example.demo.enums.TipoNotification;

import lombok.Getter;
import lombok.Setter;

@Document(collection = "notifications")
@Getter
@Setter
public class Notification {

    @Id
    private String id;

    private Long usuarioId;
    private Long organizadorId;
    private Long eventoId;
    private String nombreEvento;
    private TipoNotification tipoNotificacion;
    private String titulo;
    private String mensaje;
    private Boolean leida = false;

    @Indexed(expireAfterSeconds = 604800) // TTL 7 días
    private LocalDateTime fechaCreacion;
}