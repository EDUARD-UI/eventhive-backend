package com.eventhive.app.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IngresosPlataformaDTO {

    private final BigDecimal comisionesVentas;          // 7% o 9% sobre compras confirmadas
    private final BigDecimal comisionesPosicionados;    // parte de lo anterior generada por eventos posicionados
    private final BigDecimal ingresosPosicionamientos;  // lo que pagan las organizaciones por posicionar
    private final BigDecimal totalIngresos;             // comisionesVentas + ingresosPosicionamientos
}
