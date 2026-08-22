package com.eventhive.app.controllers;

import com.eventhive.app.dto.request.ModeracionEventoRequest;
import com.eventhive.app.dto.response.EventoDTO;
import com.eventhive.app.dto.response.ModeracionEventoDTO;
import com.eventhive.app.service.ServiceEvento;
import com.eventhive.app.service.ServiceModeracion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.response.UsuarioDTO;
import com.eventhive.app.service.ServiceUsuario;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/moderadores")
@RequiredArgsConstructor
public class ModeracionesApiController {

    private final ServiceUsuario serviceUsuario;
    private final ServiceEvento serviceEvento;
    private final ServiceModeracion serviceModeracion;

    //gestion de moderadores
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<PagedResponse<UsuarioDTO>>> listarModeradores(Pageable pageable) {
        Page<UsuarioDTO> page = serviceUsuario.obtenerModeradoresDTO(pageable);
        PagedResponse<UsuarioDTO> response = new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );

        return ResponseEntity.ok(ApiResponse.ok("Moderadores obtenidos", response));
    }

    @PutMapping("/{id}/revocar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> revocarModerador(@PathVariable Long id) {
        serviceUsuario.revocarModerador(id);
        return ResponseEntity.ok(ApiResponse.ok("Rol de moderador revocado correctamente"));
    }

    @PutMapping("/{id}/asignar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> asignarModerador(@PathVariable Long id) {
        serviceUsuario.asignarModerador(id);
        return ResponseEntity.ok(ApiResponse.ok("Rol de moderador asigando correctamente"));
    }

    // Moderación a eventos
    @GetMapping("/moderacion/pendientes")
    @PreAuthorize("hasRole('MODERADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<PagedResponse<EventoDTO>>> pendientesRevision(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Eventos pendientes de revisión",
                serviceEvento.toPagedDTO(serviceModeracion.listarPendientesRevision(pageable))));
    }

    @GetMapping("/{id}/moderaciones")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<ModeracionEventoDTO>>> historialModeracion(
            @PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Historial de moderación",
                serviceModeracion.listarModeraciones(id, pageable)));
    }

    @PatchMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('MODERADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<EventoDTO>> aprobar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Evento aprobado y publicado",
                serviceEvento.toDTO(serviceModeracion.aprobarEvento(id))));
    }

    @PatchMapping("/{id}/solicitar-correccion")
    @PreAuthorize("hasRole('MODERADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<EventoDTO>> solicitarCorreccion(
            @PathVariable Long id,
            @RequestBody ModeracionEventoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Se solicitaron correcciones al organizador",
                serviceEvento.toDTO(serviceModeracion.solicitarCorreccion(id, request.getMotivo(), request.getObservacion()))));
    }

    @PatchMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('MODERADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<EventoDTO>> rechazar(
            @PathVariable Long id,
            @RequestBody ModeracionEventoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Evento rechazado",
                serviceEvento.toDTO(serviceModeracion.rechazarEvento(id, request.getMotivo(), request.getObservacion()))));
    }
}
