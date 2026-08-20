package com.eventhive.app.controllers;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.response.OrganizacionDTO;
import com.eventhive.app.dto.request.CambiarNivelRequest;
import com.eventhive.app.service.ServiceNivelOrganizacion;
import com.eventhive.app.service.ServiceOrganizacion;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/organizaciones")
public class OrganizacionApiController {

    private final ServiceOrganizacion serviceOrganizacion;
    private final ServiceNivelOrganizacion serviceNivelOrganizacion;

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

    @PatchMapping("/{id}/nivel")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> cambiarNivel(@PathVariable Long id,
                                                          @Valid @RequestBody CambiarNivelRequest request) {
        serviceNivelOrganizacion.aprobarAscenso(id);
        return ResponseEntity.ok(ApiResponse.ok("Nivel actualizado"));
    }
}
