package com.eventhive.app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoCarruselDTO {
        //hola render
    @JsonProperty("url_imagen_promocionado")
    private String urlImagenPromocionado;
}
