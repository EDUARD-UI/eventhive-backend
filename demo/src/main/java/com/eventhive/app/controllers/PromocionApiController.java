package com.eventhive.app.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.request.PosicionamientoSeoRequest;
import com.eventhive.app.service.ServicePromocion;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/promociones/eventos")
public class PromocionApiController {

    private final ServicePromocion servicePromocion;

    @PostMapping("/{eventoId}/posicionar")
    @PreAuthorize("hasRole('REPRESENTANTE')")
    public ResponseEntity<ApiResponse<Void>> asignarImagenDestacada(
            @PathVariable Long eventoId, @Valid @RequestBody PosicionamientoSeoRequest request) {
        servicePromocion.asignarImagenDestacada(eventoId, request.getUrlImagenDestacado());
        return ResponseEntity.ok(ApiResponse.ok("Imagen destacada asignada al evento promocionado"));
    }
}
