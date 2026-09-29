package com.eventhive.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoriaDTO {
    private Long id;
    private String nombre;
    private String imagenUrl;
    private Long totalEventos;

    // Alias temporal para no romper consumidores que aún leen "urlFoto"
    @Deprecated
    @JsonProperty("urlFoto")
    public String getUrlFoto() {
        return imagenUrl;
    }
}
