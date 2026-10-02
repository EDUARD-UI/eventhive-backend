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
public class EventosResumenOrganizacionDTO {
    private long activos;
    private long borrador;
    private long finalizados;
    private long total;
}
