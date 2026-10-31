package com.eventhive.app.recommendation.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EvaluacionRecomendacionResponse {
    private List<RecomendacionResultadoDTO> recomendaciones;
}

