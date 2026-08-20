package com.eventhive.app.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CategoriaEventosDTO {
    private Long id;
    private String nombre;
    private List<EventoDTO> eventos;
    private int totalEventos;
    
}
