package com.eventhive.app.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValoracionUpdateRequest {
    @NotNull
    private Long organizacionId;

    @NotBlank
    @Size(max = 500)
    private String comentario;

    @Min(1) @Max(5)
    private long calificacion;
}
