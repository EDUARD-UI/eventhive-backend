package com.eventhive.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "banners_home", uniqueConstraints = {
        @UniqueConstraint(name = "uk_banners_home_posicion", columnNames = "posicion")
})
@Getter
@Setter
public class BannerHome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(name = "imagen_url", length = 1000)
    private String imagenUrl;

    @Column(name = "texto_boton", nullable = false, length = 80)
    private String textoBoton;

    @Column(name = "enlace_url", nullable = false, length = 2048)
    private String enlaceUrl;

    @Column(nullable = false)
    private Integer posicion;
}