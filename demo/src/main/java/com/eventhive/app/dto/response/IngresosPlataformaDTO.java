package com.eventhive.app.dto.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngresosPlataformaDTO {

    // 12.2. Exclusivamente las comisiones comerciales de EventHive por venta de boletas
    private BigDecimal comisionesVentas;

    // Parte de las comisiones generadas por eventos posicionados
    private BigDecimal comisionesPosicionados;

    // 12.3. Pagos de promociones confirmados
    private BigDecimal ingresosPagoPromocion;

    // 12.1. Venta bruta de boletas (con comisión incluida) + Pagos confirmados de promociones
    private BigDecimal ventaBrutaTotal;

    // Total de ingresos oficiales de EventHive (comisiones comerciales de boletas + ingresos por promociones)
    private BigDecimal totalIngresos;

    // Importe base correspondiente a las organizaciones por venta de boletas
    private BigDecimal ingresosOrganizaciones;

    // Constructor de compatibilidad previa
    public IngresosPlataformaDTO(BigDecimal comisionesVentas, BigDecimal comisionesPosicionados,
                                 BigDecimal ingresosPosicionamientos, BigDecimal totalIngresos) {
        this.comisionesVentas = comisionesVentas;
        this.comisionesPosicionados = comisionesPosicionados;
        this.ingresosPagoPromocion = ingresosPosicionamientos;
        this.totalIngresos = totalIngresos;
        this.ventaBrutaTotal = totalIngresos;
        this.ingresosOrganizaciones = BigDecimal.ZERO;
    }

    @JsonProperty("ingresosPosicionamientos")
    public BigDecimal getIngresosPosicionamientos() {
        return ingresosPagoPromocion;
    }
}
