package com.eventhive.app.controllers;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.response.PlanPromocionDTO;
import com.eventhive.app.service.ServiceMarketing;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/promociones")
@RequiredArgsConstructor
public class PlanPromocionApiController {

    private final ServiceMarketing service;

    // GET /api/promociones?page=0&size=10
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PlanPromocionDTO>>> listarPlanesPaginados(Pageable pageable) {
        Page<PlanPromocionDTO> page = service.listarPlanesDisponibles(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Promociones obtenidas",
                new PagedResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                        page.getTotalElements(), page.getTotalPages())));
    }

    // GET /api/promociones/planes
    @GetMapping("/planes")
    public ResponseEntity<ApiResponse<List<PlanPromocionDTO>>> listarPlanesDisponibles() {
        return ResponseEntity.ok(ApiResponse.ok("Planes disponibles", service.listarPlanesDisponibles()));
    }
}

