package com.eventhive.app.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

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

    @PostMapping(value = "/{eventoId}/posicionar", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('MARKETING','ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> asignarImagenDestacada(
            @PathVariable Long eventoId, @Valid @RequestBody PosicionamientoSeoRequest request) {
        servicePromocion.asignarImagenDestacada(eventoId, request.getUrlImagenDestacado());
        return ResponseEntity.ok(ApiResponse.ok("Imagen destacada asignada al evento promocionado"));
    }

    @PostMapping(value = "/{eventoId}/posicionar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('MARKETING','ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Map<String, String>>> asignarImagenDestacadaArchivo(
            @PathVariable Long eventoId,
            @RequestPart("imagen") MultipartFile imagen) {
        String url = servicePromocion.subirYAsignarImagenDestacada(eventoId, imagen);
        return ResponseEntity.ok(ApiResponse.ok("Imagen destacada subida y asignada al evento promocionado",
                Map.of("urlImagenDestacado", url)));
    }
}
