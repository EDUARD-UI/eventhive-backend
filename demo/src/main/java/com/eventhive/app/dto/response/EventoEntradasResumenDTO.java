package com.eventhive.app.dto.response;

import java.math.BigDecimal;
import java.util.List;
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
    private long boletasVendidas;
    private BigDecimal ingresosGenerados;
    private List<LocalidadEntradasDTO> localidades;
}
