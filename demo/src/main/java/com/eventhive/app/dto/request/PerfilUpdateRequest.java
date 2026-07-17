package com.eventhive.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerfilUpdateRequest {

    @NotBlank
    @Size(max = 150)
    private String nombre;

    @NotBlank @Pattern(regexp = "^[0-9+ ]{7,20}$")
    private String telefono;
}