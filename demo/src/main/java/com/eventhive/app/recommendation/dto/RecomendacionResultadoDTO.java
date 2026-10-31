package com.eventhive.app.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecomendacionResultadoDTO {
    private Long usuarioId;
    private Integer puntuacion;
    private String afinidad;
}

