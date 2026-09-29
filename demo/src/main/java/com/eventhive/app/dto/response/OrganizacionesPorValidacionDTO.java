package com.eventhive.app.dto.response;

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
public class OrganizacionesPorValidacionDTO {
    private long pendientesValidacionRut;   // RUT enviado, esperando revisión (solicitud en PENDIENTE)
    private long sinRut;                    // registradas que aún no han subido el RUT (solicitud INCOMPLETA)
    private long total;                     // total de organizaciones
    private List<OrganizacionEstadoConteoDTO> porEstado;
}
