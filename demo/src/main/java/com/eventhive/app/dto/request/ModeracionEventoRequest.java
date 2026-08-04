package com.eventhive.app.dto.request;

import com.eventhive.app.enums.MotivosRechazos;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModeracionEventoRequest {
    private MotivosRechazos motivo;

    @NotBlank
    private String observacion;
}
