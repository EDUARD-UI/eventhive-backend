package com.eventhive.app.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.response.SugerenciaAscensoDTO;
import com.eventhive.app.service.ServiceNivelOrganizacion;

import lombok.RequiredArgsConstructor;


//el admin podra aprobar q organizacion sube de nivel
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ascensos")
public class NivelOrganizacionApiController {

    private final ServiceNivelOrganizacion serviceNivelOrganizacion;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Page<SugerenciaAscensoDTO>>> listarPendientes(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Sugerencias de ascenso pendientes",
                serviceNivelOrganizacion.listarPendientes(pageable)));
    }

    @PatchMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> aprobar(@PathVariable Long id) {
        serviceNivelOrganizacion.aprobarAscenso(id);
        return ResponseEntity.ok(ApiResponse.ok("Ascenso aprobado"));
    }

    @PatchMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> rechazar(@PathVariable Long id) {
        serviceNivelOrganizacion.rechazarAscenso(id);
        return ResponseEntity.ok(ApiResponse.ok("Ascenso rechazado"));
    }
}
