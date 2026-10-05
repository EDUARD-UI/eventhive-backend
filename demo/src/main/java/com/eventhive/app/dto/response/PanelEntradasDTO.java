package com.eventhive.app.dto.response;

import java.math.BigDecimal;
import com.eventhive.app.dto.PagedResponse;
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
public class PanelEntradasDTO {
    private long totalEventos;
    private long totalBoletasVendidas;
    private BigDecimal totalIngresos;
    private PagedResponse<EventoEntradasResumenDTO> eventos;
}
