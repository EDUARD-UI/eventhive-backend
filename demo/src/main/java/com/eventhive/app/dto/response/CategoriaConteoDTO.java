package com.eventhive.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoriaConteoDTO {
    private final Long id;
    private final String nombre;
    private final Long totalEventos;

    public CategoriaConteoDTO(Long id, String nombre, Long totalEventos) {
        this.id = id;
        this.nombre = nombre;
        this.totalEventos = totalEventos;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Long getTotalEventos() {
        return totalEventos;
    }
}