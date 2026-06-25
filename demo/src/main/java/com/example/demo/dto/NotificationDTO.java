package com.example.demo.dto;

import java.time.LocalDateTime;

import com.example.demo.enums.TipoNotification;

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