package com.eventhive.app.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CandidatoRecomendacionDTO {
    private Long usuarioId;
    private String nombreCompleto;
    private String correo;
    private Long favoritosCategoria;
    private Long comprasCategoria;
    private Long entradasCompradas;
}

