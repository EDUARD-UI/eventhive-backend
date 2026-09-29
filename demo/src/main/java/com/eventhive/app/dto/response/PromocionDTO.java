package com.eventhive.app.dto.response;

import com.eventhive.app.enums.EstadoPromocion;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class PromocionDTO {
    private Long id;
    private String descripcion;
    private BigDecimal descuento;
    private LocalDate fechaInicio;
    private LocalDate fechaFinal;
    private EstadoPromocion estado;
    private Long eventoId;
    private String eventoTitulo;
    // Mismo valor que eventoTitulo; nombre alineado con el panel de administración
    private String eventoNombre;
}
