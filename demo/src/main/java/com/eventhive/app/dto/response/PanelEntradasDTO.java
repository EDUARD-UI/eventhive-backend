package com.eventhive.app.dto.response;

import java.math.BigDecimal;
import com.eventhive.app.dto.PagedResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private long totalEventosActivos;
    @JsonProperty("totalBoletasVendidasPorEventosPublicados")
    private long totalBoletasVendidasPorEventosPublicados;
    private BigDecimal totalIngresos;
    private PagedResponse<EventoEntradasResumenDTO> eventos;

    // Getter alias para compatibilidad con clientes existentes
    @JsonProperty("totalBoletasVendidas")
    public long getTotalBoletasVendidas() {
        return totalBoletasVendidasPorEventosPublicados;
    }

    public void setTotalBoletasVendidas(long val) {
        this.totalBoletasVendidasPorEventosPublicados = val;
    }
}
