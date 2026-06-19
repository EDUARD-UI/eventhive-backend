package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoletosCompraDTO {

    private Long id;
    private LocalDateTime fechaCompra;
    private BigDecimal total;
    private String metodoPago;
    private List<BoletoDTO> tiqueteCompras;

    @Getter
    @Setter
    public static class BoletoDTO {
        private Long id;
        private TiqueteDTO tiquete;
    }

    @Getter
    @Setter
    public static class TiqueteDTO {
        private Long id;
        private String codigoQR;
        private LocalidadDTO localidad;
    }

    @Getter
    @Setter
    public static class LocalidadDTO {
        private Long id;
        private String nombre;
        private BigDecimal precio;
        private EventoDTO evento;
    }

    @Getter
    @Setter
    public static class EventoDTO {
        private Long id;
        private String titulo;
        private LocalDate fecha;
        private LocalTime hora;
        private String lugar;
    }
}