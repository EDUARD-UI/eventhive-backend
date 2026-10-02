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
public class LocalidadEntradasDTO {
    private Long id;
    private String nombre;
    private BigDecimal precio;
    private int capacidad;
    private int disponibles;
    private long boletasVendidas;
    private double porcentajeVendido;
}
