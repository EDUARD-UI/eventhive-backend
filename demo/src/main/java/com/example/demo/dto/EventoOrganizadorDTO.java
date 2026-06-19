package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventoOrganizadorDTO {
    private Long id;
    private String nombre;
    private Boolean esVerificado;

    public EventoOrganizadorDTO(Long id, String nombre, Boolean esVerificado) {
        this.id = id;
        this.nombre = nombre;
        this.esVerificado = esVerificado;
    }
}