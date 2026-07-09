package com.eventhive.app.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.OrganizacionDTO;
import com.eventhive.app.service.ServiceOrganizacion;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/organizaciones")
public class OrganizacionApiController {

    private final ServiceOrganizacion serviceOrganizacion;

    // Perfil + estadísticas de la organización autenticada
    @GetMapping("/mi-organizacion")
    @PreAuthorize("hasRole('ORGANIZACION')")
    public ResponseEntity<ApiResponse<OrganizacionDTO>> miOrganizacion() {
        return ResponseEntity.ok(ApiResponse.ok("Organización obtenida", serviceOrganizacion.miOrganizacion()));
    }

    // Consulta administrativa del perfil de cualquier organización
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<OrganizacionDTO>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Organización obtenida", serviceOrganizacion.obtenerPorId(id)));
    }
}
