package com.eventhive.app.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PromocionRequest {
    @NotNull
    private Long eventoId;

    @NotBlank
    @Size(max = 300) private String descripcion;

    @NotNull @DecimalMin(value = "1", inclusive = true) @DecimalMax("75")
    private BigDecimal descuento;

    @NotNull @FutureOrPresent
    private LocalDate fechaInicio;

    @NotNull @Future
    private LocalDate fechaFin;
}
