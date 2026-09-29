package com.eventhive.app.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopEventoVentasDTO {
    private Long eventoId;
    private String nombre;
    private Long organizacionId;
    private String organizacion;
    private long entradasVendidas;
    private BigDecimal totalVentas;
}
