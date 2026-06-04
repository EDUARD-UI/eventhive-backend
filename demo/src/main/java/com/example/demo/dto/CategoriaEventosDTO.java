package com.example.demo.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoriaEventosDTO {
    private String id;
    private String nombre;
    private List<EventoDTO> eventos;
    private int totalEventos;
    
}
