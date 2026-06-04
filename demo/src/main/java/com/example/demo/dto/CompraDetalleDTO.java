package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class CompraDetalleDTO {
    private String id;
    private LocalDateTime fechaCompra;
    private BigDecimal total;
    private String metodoPago;
    private Integer cantidad;
    private String localidadNombre;
    private String eventoTitulo;
    private LocalDate eventoFecha;
    private LocalTime eventoHora;
    private String eventoLugar;
    
    public CompraDetalleDTO() {
    }
}