package com.eventhive.app.dto;

import com.eventhive.app.enums.EstadoCompra;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CompraResponseDTO {

    private Long id;
    private LocalDateTime fechaCompra;
    private BigDecimal total;
    private String metodoPago;
    private EstadoCompra estado;
    private List<ItemCompraDTO> items;
}