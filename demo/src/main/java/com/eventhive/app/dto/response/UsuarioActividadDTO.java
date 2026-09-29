package com.eventhive.app.dto.response;

import java.math.BigDecimal;

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
public class UsuarioActividadDTO {
    private long comprasConfirmadas;
    private long entradasCompradas;
    private long eventosComprados;
    private long favoritos;
    private BigDecimal totalGastado;
}
