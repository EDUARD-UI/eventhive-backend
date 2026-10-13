package com.eventhive.app.dto.response;

import java.math.BigDecimal;

import com.eventhive.app.enums.EstadoEvento;

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
public class EventoEntradasResumenDTO {
    private Long id;
    private String nombre;
    private EstadoEvento estado;
    private long boletasVendidas;
    private BigDecimal ingresosGenerados;
}
