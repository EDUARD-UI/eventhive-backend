package com.eventhive.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventoBusquedaDTO {
    private Long id;
    private String titulo;
    private String nombreCategoria;
    private LocalDate fecha;
}
