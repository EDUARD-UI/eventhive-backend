package com.eventhive.app.dto.response;

import java.math.BigDecimal;

import com.eventhive.app.enums.EstadoPosicionamiento;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PagoPosicionamientoDTO {
    private Long id;
    private Long eventoId;
    private Long organizacionId;
    private BigDecimal precio;
    private EstadoPosicionamiento estado;
}