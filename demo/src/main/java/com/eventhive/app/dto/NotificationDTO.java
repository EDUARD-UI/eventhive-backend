package com.eventhive.app.dto;

import java.time.LocalDateTime;

import com.eventhive.app.enums.TipoNotification;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationDTO {
    private String id;
    private String titulo;
    private String mensaje;
    private TipoNotification tipoNotificacion;
    private Long eventoId;
    private String nombreEvento;
    private Boolean leida;
    private LocalDateTime fechaCreacion;
}