package com.eventhive.app.dto.response;

import com.eventhive.app.enums.EstadoOrganizacion;

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
public class OrganizacionEstadoConteoDTO {
    private EstadoOrganizacion estado;
    private long cantidad;
}
