package com.eventhive.app.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class LocalidadDTO {
    private Long id;
    private String nombre;
    private BigDecimal precio;
    private int capacidad;
    private int disponibles;
}