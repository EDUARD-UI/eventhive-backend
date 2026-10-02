package com.eventhive.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eventhive.app.model.BannerHome;

public interface BannerHomeRepository extends JpaRepository<BannerHome, Long> {

    List<BannerHome> findAllByOrderByPosicionAsc();

    Optional<BannerHome> findByPosicion(Integer posicion);
}