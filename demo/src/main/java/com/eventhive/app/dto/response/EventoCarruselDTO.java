package com.eventhive.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventoCarruselDTO {

    private Long id;

    @JsonProperty("url_imagen_destacado")
    private String urlImagenDestacado;

    @JsonProperty("url_imagen_promocionado")
    private String urlImagenPromocionado;

    public EventoCarruselDTO(String urlImagenDestacado) {
        this.urlImagenDestacado = urlImagenDestacado;
        this.urlImagenPromocionado = urlImagenDestacado;
    }

    public EventoCarruselDTO(Long id, String urlImagenDestacado) {
        this.id = id;
        this.urlImagenDestacado = urlImagenDestacado;
        this.urlImagenPromocionado = urlImagenDestacado;
    }
}
