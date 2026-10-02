package com.eventhive.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BannerHomeDTO {

    private Long id;
    private String titulo;
    private String imagenUrl;
    private String textoBoton;
    private String enlaceUrl;
    private Integer posicion;
}