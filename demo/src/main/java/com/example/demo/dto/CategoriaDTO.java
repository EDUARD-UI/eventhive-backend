package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoriaDTO{

    private String id;
    private String nombre;

    public CategoriaDTO() {
    }

    public CategoriaDTO(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }
}