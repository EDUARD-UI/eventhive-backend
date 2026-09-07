package com.eventhive.app.controllers;

import com.eventhive.app.dto.request.ModeracionEventoRequest;
import com.eventhive.app.dto.response.EventoDTO;
import com.eventhive.app.dto.response.ModeracionEventoDTO;
import com.eventhive.app.service.ServiceEvento;
import com.eventhive.app.service.ServiceModeracion;
import jakarta.validation.Valid;
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
@RequestMapping("/api/moderaciones")
@RequiredArgsConstructor
public class ModeracionesApiController {

    private final ServiceUsuario serviceUsuario;
    private final ServiceEvento serviceEvento;
    private final ServiceModeracion serviceModeracion;

    //GESTION DE MODERADORES
    @GetMapping("/moderadores")
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

    @PutMapping("moderadores/{moderadorId}/revocar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> revocarModerador(@PathVariable Long moderadorId) {
        serviceUsuario.revocarModerador(moderadorId);
        return ResponseEntity.ok(ApiResponse.ok("Rol de moderador revocado correctamente"));
    }

    @PutMapping("moderadores/{moderadorId}/asignar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> asignarModerador(@PathVariable Long moderadorId) {
        serviceUsuario.asignarModerador(moderadorId);
        return ResponseEntity.ok(ApiResponse.ok("Rol de moderador asigando correctamente"));
    }

    // MODERACION A EVENTOS
    @GetMapping("/eventos/pendientes")
    @PreAuthorize("hasRole('MODERADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<PagedResponse<EventoDTO>>> pendientesRevision(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Eventos pendientes de revisión",
                serviceEvento.toPagedDTO(serviceModeracion.listarPendientesRevision(pageable))));
    }

    @GetMapping("/eventos/{eventoId}/moderaciones")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<ModeracionEventoDTO>>> historialModeracion(
            @PathVariable Long eventoId, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Historial de moderación",
                serviceModeracion.historialDeModeraciones(eventoId, pageable)));
    }

    @PatchMapping("/eventos/{eventoId}/aprobar")
    @PreAuthorize("hasRole('MODERADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<EventoDTO>> aprobar(@PathVariable Long eventoId) {
        return ResponseEntity.ok(ApiResponse.ok("Evento aprobado y publicado",
                serviceEvento.toDTO(serviceModeracion.aprobarEvento(eventoId))));
    }

    @PatchMapping("/eventos/{eventoId}/solicitar-correccion")
    @PreAuthorize("hasRole('MODERADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<EventoDTO>> solicitarCorreccion(
            @PathVariable Long eventoId,
            @RequestBody @Valid ModeracionEventoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Se solicitaron correcciones al organizador",
                serviceEvento.toDTO(serviceModeracion.solicitarCorreccion(eventoId, request.getMotivo(), request.getObservacion()))));
    }

    @PatchMapping("/eventos/{eventoId}/rechazar")
    @PreAuthorize("hasRole('MODERADOR') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<EventoDTO>> rechazar(
            @PathVariable Long eventoId,
            @RequestBody @Valid ModeracionEventoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Evento rechazado",
                serviceEvento.toDTO(serviceModeracion.rechazarEvento(eventoId, request.getMotivo(), request.getObservacion()))));
    }

    @PatchMapping("/eventos/{eventoId}/suspender")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<EventoDTO>> suspender(
            @PathVariable Long eventoId,
            @RequestBody @Valid ModeracionEventoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Evento suspendido",
                serviceEvento.toDTO(serviceModeracion.suspenderEvento(eventoId, request.getMotivo(), request.getObservacion()))));
    }

    @PatchMapping("/eventos/{eventoId}/reactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<EventoDTO>> reactivar(@PathVariable Long eventoId) {
        return ResponseEntity.ok(ApiResponse.ok("Evento ractivado y publicado exitosamente",
                serviceEvento.toDTO(serviceModeracion.reactivarEvento(eventoId))));
    }
}
