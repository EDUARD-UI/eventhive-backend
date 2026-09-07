package com.eventhive.app.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.request.PermisosOperadorRequest;
import com.eventhive.app.dto.response.InvitacionOrganizacionDTO;
import com.eventhive.app.dto.response.OperadorDTO;
import com.eventhive.app.dto.response.OrganizacionDTO;
import com.eventhive.app.dto.response.OrganizacionPublicaDTO;
import com.eventhive.app.dto.response.SugerenciaAscensoDTO;
import com.eventhive.app.dto.response.UsuarioDTO;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.service.ServiceInvitacionOrganizacion;
import com.eventhive.app.service.ServiceNivelOrganizacion;
import com.eventhive.app.service.ServiceOrganizacion;
import com.eventhive.app.service.ServiceSeguidor;
import com.eventhive.app.service.ServiceUsuario;
import com.eventhive.app.utils.AuthenticatedUserHelper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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
    public ResponseEntity<ApiResponse<PagedResponse<OrganizacionPublicaDTO>>> listar(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Organizaciones obtenidas",
                serviceUsuario.toPagedOrganizacion(serviceUsuario.obtenerOrganizaciones(pageable))));
    }

    @GetMapping("/{organizacionId}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<OrganizacionDTO>> obtener(@PathVariable Long organizacionId) {
        return ResponseEntity.ok(ApiResponse.ok("Organización obtenida", serviceOrganizacion.obtenerPorId(organizacionId)));
    }

    @GetMapping("/top")
    public ResponseEntity<ApiResponse<PagedResponse<OrganizacionPublicaDTO>>> topOrganizaciones(Pageable pageable) {
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

    @GetMapping("/invitaciones-enviadas")
    @PreAuthorize("hasRole('REPRESENTANTE')")
    public ResponseEntity<ApiResponse<PagedResponse<InvitacionOrganizacionDTO>>> invitacionesEnviadas(Pageable pageable) {
        Page<InvitacionOrganizacionDTO> page = serviceInvitacion.listarInvitacionesOrganizacion(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Invitaciones de la organización obtenidas",
                new PagedResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                        page.getTotalElements(), page.getTotalPages())));
    }

    @GetMapping("/mis-operadores")
    @PreAuthorize("hasRole('REPRESENTANTE')")
    public ResponseEntity<ApiResponse<PagedResponse<OperadorDTO>>> listarOperadores(Pageable pageable) {
        Page<OperadorDTO> page = serviceOrganizacion.listarOperadores(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Operadores obtenidos",
                new PagedResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                        page.getTotalElements(), page.getTotalPages())));
    }

    @GetMapping("/mi-organizacion/seguidores")
    @PreAuthorize("hasRole('REPRESENTANTE') or hasRole('OPERADOR')")
    public ResponseEntity<ApiResponse<PagedResponse<UsuarioDTO>>> misSeguidores(Pageable pageable) {
        Usuario usuario = authHelper.usuarioAutenticado();
        if (usuario.getOrganizacion() == null) {
            throw new BusinessException("El usuario no tiene una organización asociada");
        }
        Long organizacionId = usuario.getOrganizacion().getId();
        Page<UsuarioDTO> page = serviceSeguidor.listarSeguidores(organizacionId, pageable);
        PagedResponse<UsuarioDTO> response = new PagedResponse<>(page.getContent(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
        return ResponseEntity.ok(ApiResponse.ok("Seguidores de la organizacion obtenidos", response));
    }

    //endpoint para obtener las valoracion de mi organizacion(crear)

    //GESTION DE PERMISOS Y OPERADORES
    @PatchMapping("/operadores/{operadorId}/actualizar-permisos")
    @PreAuthorize("hasRole('REPRESENTANTE')")
    public ResponseEntity<ApiResponse<Void>> actualizarPermisos(
            @PathVariable Long operadorId,
            @Valid @RequestBody PermisosOperadorRequest request) {
        serviceOrganizacion.actualizarPermisos(operadorId, request);
        return ResponseEntity.ok(ApiResponse.ok("Permisos actualizados"));
    }

    @DeleteMapping("/expulsar-operador/{operadorId}")
    @PreAuthorize("hasRole('REPRESENTANTE')")
    public ResponseEntity<ApiResponse<Void>> expulsarOperador(@PathVariable Long operadorId) {
        serviceOrganizacion.expulsarOperador(operadorId);
        return ResponseEntity.ok(ApiResponse.ok("Operador removido de la organización"));
    }

    @PostMapping("/invitar")
    @PreAuthorize("hasRole('REPRESENTANTE')")
    public ResponseEntity<ApiResponse<Void>> invitar(@RequestParam String correo) {
        serviceInvitacion.invitar(correo);
        return ResponseEntity.ok(ApiResponse.ok("Invitación enviada"));
    }

    @PatchMapping("invitaciones/{invitacionId}/aceptar")
    public ResponseEntity<ApiResponse<Void>> aceptarInvitacion(@PathVariable Long invitacionId) {
        serviceInvitacion.aceptar(invitacionId);
        return ResponseEntity.ok(ApiResponse.ok("Invitación aceptada"));
    }

    @PatchMapping("invitaciones/{operadorId}/rechazar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> rechazarInvitacion(@PathVariable Long operadorId) {
        serviceInvitacion.rechazar(operadorId);
        return ResponseEntity.ok(ApiResponse.ok("Invitación rechazada"));
    }

    //GESTION DE SEGERENCIAS DE ASCENSO
    @GetMapping("/sugerencias-pendientes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Page<SugerenciaAscensoDTO>>> listarSugerenciasPendientes(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Sugerencias de ascenso pendientes",
                serviceNivelOrganizacion.listarPendientes(pageable)));
    }

    @PatchMapping("/{sugerenciaId}/aprobar-sugerencia")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> aprobarSugerencia(@PathVariable Long sugerenciaId) {
        serviceNivelOrganizacion.aprobarAscenso(sugerenciaId);
        return ResponseEntity.ok(ApiResponse.ok("Ascenso aprobado"));
    }

    @PatchMapping("/{sugerenciaId}/rechazar-sugerencia")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> rechazarSugerencia(@PathVariable Long sugerenciaId) {
        serviceNivelOrganizacion.rechazarAscenso(sugerenciaId);
        return ResponseEntity.ok(ApiResponse.ok("Ascenso rechazado"));
    }
}
