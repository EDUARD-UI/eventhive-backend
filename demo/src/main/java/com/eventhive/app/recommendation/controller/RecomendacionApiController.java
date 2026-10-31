package com.eventhive.app.recommendation.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.response.EventoDTO;
import com.eventhive.app.recommendation.service.ServicioRecomendacion;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/recommendations", "/api/recomendaciones"})
public class RecomendacionApiController {

    private final ServicioRecomendacion servicioRecomendacion;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<EventoDTO>>> obtenerMisRecomendaciones() {
        List<EventoDTO> eventos = servicioRecomendacion.obtenerRecomendacionesParaUsuarioAutenticado();
        return ResponseEntity.ok(ApiResponse.ok("Eventos recomendados obtenidos", eventos));
    }
}

