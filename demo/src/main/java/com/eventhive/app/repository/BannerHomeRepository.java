package com.eventhive.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eventhive.app.model.BannerHome;

public interface BannerHomeRepository extends JpaRepository<BannerHome, Long> {

    // Obtiene todos los banners de la página principal ordenados por su posición de forma ascendente.
    List<BannerHome> findAllByOrderByPosicionAsc();

    // Busca un banner de la página principal por su número de posición.
    Optional<BannerHome> findByPosicion(Integer posicion);
}