package com.eventhive.app.controllers;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.request.PermisosOperadorRequest;
import com.eventhive.app.dto.response.*;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.service.*;
import com.eventhive.app.utils.AuthenticatedUserHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/organizaciones")
public class OrganizacionApiController {

    private final AuthenticatedUserHelper authHelper;
    private final ServiceOrganizacion serviceOrganizacion;
    private final ServiceNivelOrganizacion serviceNivelOrganizacion;
    private final ServiceSeguidor serviceSeguidor;
    private final ServiceInvitacionOrganizacion serviceInvitacion;
    private final ServiceUsuario serviceUsuario;

    //CONSULTAS
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<OrganizacionDTO>>> listar(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Organizaciones obtenidas",
                serviceUsuario.toPagedOrganizacion(serviceUsuario.obtenerOrganizaciones(pageable))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<OrganizacionDTO>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Organización obtenida", serviceOrganizacion.obtenerPorId(id)));
    }

    @GetMapping("/top")
    public ResponseEntity<ApiResponse<PagedResponse<OrganizacionDTO>>> topOrganizaciones(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Top de organizaciones",
                serviceUsuario.toPagedOrganizacion(serviceUsuario.obtenerTopOrganizaciones(pageable))));
    }

    @GetMapping("/mi-organizacion")
    @PreAuthorize("hasRole('REPRESENTANTE')")
    public ResponseEntity<ApiResponse<OrganizacionDTO>> miOrganizacion() {
        return ResponseEntity.ok(ApiResponse.ok("Organización obtenida", serviceOrganizacion.miOrganizacion()));
    }

    @GetMapping("/mis-invitaciones")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PagedResponse<InvitacionOrganizacionDTO>>> misInvitaciones(Pageable pageable) {
        Page<InvitacionOrganizacionDTO> page = serviceInvitacion.misInvitacionesPendientes(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Invitaciones pendientes obtenidas",
                new PagedResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                        page.getTotalElements(), page.getTotalPages())));
    }

    @GetMapping("operadores/invitaciones")
    @PreAuthorize("hasRole('REPRESENTANTE')")
    public ResponseEntity<ApiResponse<PagedResponse<InvitacionOrganizacionDTO>>> invitacionesEnviadas(Pageable pageable) {
        Page<InvitacionOrganizacionDTO> page = serviceInvitacion.listarInvitacionesOrganizacion(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Invitaciones de la organización obtenidas",
                new PagedResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                        page.getTotalElements(), page.getTotalPages())));
    }

    @GetMapping("/operadores")
    @PreAuthorize("hasRole('REPRESENTANTE')")
    public ResponseEntity<ApiResponse<PagedResponse<OperadorDTO>>> listarOperadores(Pageable pageable) {
        Page<OperadorDTO> page = serviceOrganizacion.listarOperadores(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Operadores obtenidos",
                new PagedResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                        page.getTotalElements(), page.getTotalPages())));
    }

    @GetMapping("/mi-organizacion/seguidores")
    @PreAuthorize("hasRole('REPRESENTANTE') or hasRole('OPERADOR')")
    public ResponseEntity<ApiResponse<PagedResponse<Usuario>>> misSeguidores(Pageable pageable) {
        Usuario usuario = authHelper.usuarioAutenticado();
        if (usuario.getOrganizacion() == null) {
            throw new BusinessException("El usuario no tiene una organización asociada");
        }
        Long organizacionId = usuario.getOrganizacion().getId();
        Page<Usuario> page = serviceSeguidor.listarSeguidores(organizacionId, pageable);
        PagedResponse<Usuario> response = new PagedResponse<>(page.getContent(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
        return ResponseEntity.ok(ApiResponse.ok("Seguidores de la organizacion obtenidos", response));
    }

    //endpoint para obtener las valoracion de mi organizacion

    //GESTION DE PERMISOS Y OPERADORES
    @PatchMapping("/operadores/{id}/permisos")
    @PreAuthorize("hasRole('REPRESENTANTE')")
    public ResponseEntity<ApiResponse<Void>> actualizarPermisos(
            @PathVariable Long id, @RequestBody PermisosOperadorRequest request) {
        serviceOrganizacion.actualizarPermisos(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Permisos actualizados"));
    }

    @DeleteMapping("/operadores/{id}")
    @PreAuthorize("hasRole('REPRESENTANTE')")
    public ResponseEntity<ApiResponse<Void>> expulsarOperador(@PathVariable Long id) {
        serviceOrganizacion.expulsarOperador(id);
        return ResponseEntity.ok(ApiResponse.ok("Operador removido de la organización"));
    }

    @PostMapping("operadores/invitar")
    @PreAuthorize("hasRole('REPRESENTANTE')")
    public ResponseEntity<ApiResponse<Void>> invitar(@RequestParam String correo) {
        serviceInvitacion.invitar(correo);
        return ResponseEntity.ok(ApiResponse.ok("Invitación enviada"));
    }

    @PatchMapping("operadores/{id}/aceptar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> aceptarInvitacion(@PathVariable Long id) {
        serviceInvitacion.aceptar(id);
        return ResponseEntity.ok(ApiResponse.ok("Invitación aceptada"));
    }

    @PatchMapping("operadores/{id}/rechazar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> rechazarInvitacion(@PathVariable Long id) {
        serviceInvitacion.rechazar(id);
        return ResponseEntity.ok(ApiResponse.ok("Invitación rechazada"));
    }

    //GESTION DE SEGERENCIAS DE ASCENSO
    @GetMapping("/sugerencias-pendientes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Page<SugerenciaAscensoDTO>>> listarSugerenciasPendientes(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Sugerencias de ascenso pendientes",
                serviceNivelOrganizacion.listarPendientes(pageable)));
    }

    @PatchMapping("/{id}/aprobar-sugerencia")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> aprobarSugerencia(@PathVariable Long id) {
        serviceNivelOrganizacion.aprobarAscenso(id);
        return ResponseEntity.ok(ApiResponse.ok("Ascenso aprobado"));
    }

    @PatchMapping("/{id}/rechazar-sugerencia")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> rechazarSugerencia(@PathVariable Long id) {
        serviceNivelOrganizacion.rechazarAscenso(id);
        return ResponseEntity.ok(ApiResponse.ok("Ascenso rechazado"));
    }
}
