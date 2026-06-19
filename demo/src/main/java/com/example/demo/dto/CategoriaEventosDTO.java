package com.example.demo.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoriaEventosDTO {
    private Long id;
    private String nombre;
    private List<EventoDTO> eventos;
    private int totalEventos;
    
}
