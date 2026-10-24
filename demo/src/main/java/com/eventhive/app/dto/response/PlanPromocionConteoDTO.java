package com.eventhive.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlanPromocionConteoDTO {
    private Long planId;
    private String planCodigo;
    private String planNombre;
    private long cantidadEventos;
}
