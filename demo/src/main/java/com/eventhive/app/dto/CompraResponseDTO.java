package com.eventhive.app.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CompraResponseDTO {

    private final Long id;
    private final LocalDateTime fechaCompra;
    private final BigDecimal total;
    private final String metodoPago;
    private final List<ItemCompraDTO> items;
}