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
public class TopOrganizacionVentasDTO {
    private Long organizacionId;
    private String razonSocial;
    private long entradasVendidas;
    private BigDecimal totalVentas;
}
