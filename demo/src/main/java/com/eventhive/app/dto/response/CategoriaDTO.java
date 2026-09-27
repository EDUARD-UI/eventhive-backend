package com.eventhive.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoriaDTO {
    private Long id;
    private String nombre;
    private String urlFoto;
    private Long totalEventos;
}