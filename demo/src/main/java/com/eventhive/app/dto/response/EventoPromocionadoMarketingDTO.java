package com.eventhive.app.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventoPromocionadoMarketingDTO {
    private Long eventoId;
    private String evento;
    private Long organizacionId;
    private String organizacion;
    private String correoRepresentante;
    private String plan;
    private BigDecimal precioPagado;
    private BigDecimal comisionPorcentaje;
    private boolean premium;
    private boolean pautaRedes;
    private LocalDateTime fechaContratacion;
}
