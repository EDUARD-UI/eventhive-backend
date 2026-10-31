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
public class EvaluacionRecomendacionRequest {
    private EventoRecomendacionDTO evento;
    private List<CandidatoRecomendacionDTO> candidatos;
}

