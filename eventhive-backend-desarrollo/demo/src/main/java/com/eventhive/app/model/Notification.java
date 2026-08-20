package com.eventhive.app.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.eventhive.app.enums.TipoNotification;

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

    @Indexed(expireAfter = "P7D") // TTL 7 días — ISO 8601
    private LocalDateTime fechaCreacion;
}
