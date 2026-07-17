package com.eventhive.app.dto.response;

import com.eventhive.app.enums.TipoNotification;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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