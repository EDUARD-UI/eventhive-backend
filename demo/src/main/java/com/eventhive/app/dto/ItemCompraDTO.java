package com.eventhive.app.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ItemCompraDTO {

    private Long localidadId;
    private String localidadNombre;
    private String eventoNombre;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}