package com.eventhive.app.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.response.PlanPromocionDTO;
import com.eventhive.app.service.ServiceMarketing;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/promociones/planes")
@RequiredArgsConstructor
public class PlanPromocionApiController {

    private final ServiceMarketing service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlanPromocionDTO>>> listarPlanesDisponibles() {
        return ResponseEntity.ok(ApiResponse.ok("Planes disponibles", service.listarPlanesDisponibles()));
    }
}
