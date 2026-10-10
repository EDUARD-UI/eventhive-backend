package com.eventhive.app.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PlanPromocionDTO {
    private Long id;
    private String codigo;
    private String nombre;
    private BigDecimal precio;
    private BigDecimal comisionPorcentaje;
    private boolean premium;
    private boolean pautaRedes;
    private String detallePublicidad;
    private boolean activo;
}
