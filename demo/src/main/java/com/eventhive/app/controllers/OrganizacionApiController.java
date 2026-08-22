package com.eventhive.app.controllers;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.response.OrganizacionDTO;
import com.eventhive.app.dto.request.CambiarNivelRequest;
import com.eventhive.app.dto.response.SugerenciaAscensoDTO;
import com.eventhive.app.dto.response.UsuarioDTO;
import com.eventhive.app.service.ServiceNivelOrganizacion;
import com.eventhive.app.service.ServiceOrganizacion;
import com.eventhive.app.service.ServiceUsuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/organizaciones")
public class OrganizacionApiController {

    private final ServiceOrganizacion serviceOrganizacion;
    private final ServiceNivelOrganizacion serviceNivelOrganizacion;
    private final ServiceUsuario serviceUsuario;

    //consultas
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<UsuarioDTO>>> listar(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Organizaciones obtenidas",
                serviceUsuario.toPaged(serviceUsuario.obtenerOrganizaciones(pageable))));
    }

    @GetMapping("/top")
    public ResponseEntity<ApiResponse<PagedResponse<OrganizacionDTO>>> topOrganizaciones(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Top de organizaciones",
                serviceUsuario.toPagedOrganizacion(serviceUsuario.obtenerTopOrganizaciones(pageable))));
    }

    @GetMapping("/mi-organizacion")
    @PreAuthorize("hasRole('ORGANIZACION')")
    public ResponseEntity<ApiResponse<OrganizacionDTO>> miOrganizacion() {
        return ResponseEntity.ok(ApiResponse.ok("Organización obtenida", serviceOrganizacion.miOrganizacion()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<OrganizacionDTO>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Organización obtenida", serviceOrganizacion.obtenerPorId(id)));
    }

    //modificaciones
    @PatchMapping("/{id}/nivel")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> cambiarNivel(@PathVariable Long id,
                                                          @Valid @RequestBody CambiarNivelRequest request) {
        serviceNivelOrganizacion.aprobarAscenso(id);
        return ResponseEntity.ok(ApiResponse.ok("Nivel actualizado"));
    }

    //gestion de niveles de organizaciones
    @GetMapping("/sugerencias-pendientes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Page<SugerenciaAscensoDTO>>> listarPendientes(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Sugerencias de ascenso pendientes",
                serviceNivelOrganizacion.listarPendientes(pageable)));
    }

    @PatchMapping("/{id}/aprobar-sugerencia")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> aprobar(@PathVariable Long id) {
        serviceNivelOrganizacion.aprobarAscenso(id);
        return ResponseEntity.ok(ApiResponse.ok("Ascenso aprobado"));
    }

    @PatchMapping("/{id}/rechazar-sugerencia")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> rechazar(@PathVariable Long id) {
        serviceNivelOrganizacion.rechazarAscenso(id);
        return ResponseEntity.ok(ApiResponse.ok("Ascenso rechazado"));
    }
}
