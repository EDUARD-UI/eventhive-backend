package com.eventhive.app.dto.request;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanPromocionRequest {

    @NotBlank(message = "El código del plan es obligatorio")
    @Size(max = 50, message = "El código no puede superar los 50 caracteres")
    private String codigo;

    @NotBlank(message = "El nombre del plan es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
    private String nombre;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.00", inclusive = true, message = "El precio no puede ser negativo")
    private BigDecimal precio;

    @DecimalMin(value = "0.00", message = "La comisión porcentual no puede ser negativa")
    @DecimalMax(value = "100.00", message = "La comisión porcentual no puede ser superior a 100")
    @Builder.Default
    private BigDecimal comisionPorcentaje = BigDecimal.ZERO;

    @Size(max = 1000, message = "El detalle de publicidad no puede superar los 1000 caracteres")
    private String detallePublicidad;

    private boolean premium;

    private boolean pautaRedes;

    @Builder.Default
    private boolean activo = true;

    @JsonProperty("llevaPautaRedes")
    public boolean isLlevaPautaRedes() {
        return pautaRedes;
    }

    @JsonProperty("llevaPautaRedes")
    public void setLlevaPautaRedes(boolean llevaPautaRedes) {
        this.pautaRedes = llevaPautaRedes;
    }

    @JsonProperty("porcentajeComision")
    public BigDecimal getPorcentajeComision() {
        return comisionPorcentaje;
    }

    @JsonProperty("porcentajeComision")
    public void setPorcentajeComision(BigDecimal porcentajeComision) {
        if (porcentajeComision != null) {
            this.comisionPorcentaje = porcentajeComision;
        }
    }
}
