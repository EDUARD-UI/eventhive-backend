package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PromocionDTO {
    private Long id;
    private String descripcion;
    private BigDecimal descuento;
    private LocalDate fechaInicio;
    private LocalDate fechaFinal;
    private Long eventoId;
    private String eventoTitulo;
}