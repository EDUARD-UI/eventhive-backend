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

    private String id;
    private LocalDateTime fechaCompra;
    private BigDecimal total;
    private String metodoPago;
    private List<BoletoDTO> tiqueteCompras;

    @Getter
    @Setter
    public static class BoletoDTO {
        private String id;
        private TiqueteDTO tiquete;
    }

    @Getter
    @Setter
    public static class TiqueteDTO {
        private String id;
        private String codigoQR;
        private LocalidadDTO localidad;
    }

    @Getter
    @Setter
    public static class LocalidadDTO {
        private String id;
        private String nombre;
        private BigDecimal precio;
        private EventoDTO evento;
    }

    @Getter
    @Setter
    public static class EventoDTO {
        private String id;
        private String titulo;
        private LocalDate fecha;
        private LocalTime hora;
        private String lugar;
    }
}