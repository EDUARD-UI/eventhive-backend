package com.eventhive.app.dto.request;

import com.eventhive.app.enums.MotivosRechazos;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModeracionEventoRequest {
    private MotivosRechazos motivo;
    private String observacion; //solo se requiere si el motivo es OTRO
}
