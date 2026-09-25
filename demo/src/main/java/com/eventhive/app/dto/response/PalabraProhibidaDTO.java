package com.eventhive.app.dto.response;

import java.time.LocalDateTime;

import com.eventhive.app.enums.SeveridadPalabra;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PalabraProhibidaDTO {
    private Long id;
    private String palabra;
    private SeveridadPalabra severidad;
    private boolean activa;
    private LocalDateTime fechaCreacion;
}