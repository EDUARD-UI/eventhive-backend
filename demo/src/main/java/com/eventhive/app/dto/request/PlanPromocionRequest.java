package com.eventhive.app.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlanPromocionRequest {

    @NotBlank
    @Size(max = 120)
    private String nombre;

    @NotNull
    @Positive
    private BigDecimal precio;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal comisionPorcentaje;

    @Size(max = 1000)
    private String detallePublicidad;

    private boolean premium;
    private boolean pautaRedes;
    private boolean activo = true;
}
