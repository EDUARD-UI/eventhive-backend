package com.eventhive.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ModeracionEstadisticasDTO {

    private long revisados;
    private long aprobados;
    private long rechazados;
    private long correccionesSolicitadas;
}