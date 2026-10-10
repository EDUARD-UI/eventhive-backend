package com.eventhive.app.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.request.PlanPromocionRequest;
import com.eventhive.app.dto.response.EventoPromocionadoMarketingDTO;
import com.eventhive.app.dto.response.PlanPromocionDTO;
import com.eventhive.app.service.ServiceMarketing;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/marketing")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MARKETING')")
public class MarketingApiController {

    private final ServiceMarketing service;

    @GetMapping("/planes")
    public ResponseEntity<ApiResponse<List<PlanPromocionDTO>>> listarPlanes() {
        return ResponseEntity.ok(ApiResponse.ok("Planes de promoción obtenidos", service.listarPlanes()));
    }

    @PutMapping("/planes/{id}")
    public ResponseEntity<ApiResponse<PlanPromocionDTO>> actualizarPlan(
            @PathVariable Long id, @Valid @RequestBody PlanPromocionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Plan actualizado", service.actualizarPlan(id, request)));
    }

    @GetMapping("/eventos-promocionados")
    public ResponseEntity<ApiResponse<List<EventoPromocionadoMarketingDTO>>> listarEventosPromocionados() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Eventos con promoción pagada obtenidos", service.listarEventosPromocionados()));
    }
}
