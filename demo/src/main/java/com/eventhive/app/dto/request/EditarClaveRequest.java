package com.eventhive.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EditarClaveRequest {

    @NotBlank
    private String claveActual;

    @NotBlank
    private String claveNueva;
}
