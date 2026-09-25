package com.eventhive.app.dto.request;

import com.eventhive.app.enums.SeveridadPalabra;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PalabraProhibidaRequest {

    @NotBlank
    @Size(max = 100)
    private String palabra;

    @NotNull
    private SeveridadPalabra severidad;
}