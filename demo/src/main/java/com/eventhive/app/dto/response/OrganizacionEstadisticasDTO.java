package com.eventhive.app.dto.response;

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
public class OrganizacionEstadisticasDTO {
    private long eventosActivos;
    private long boletasVendidas;
    private long totalEventos;
}
