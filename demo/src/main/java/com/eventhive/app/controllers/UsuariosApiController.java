package com.eventhive.app.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.request.ActualizarPerfilRequest;
import com.eventhive.app.dto.request.EditarClaveRequest;
import com.eventhive.app.dto.response.OrganizacionPublicaDTO;
import com.eventhive.app.dto.response.UsuarioActividadDTO;
import com.eventhive.app.dto.response.UsuarioAdminDTO;
import com.eventhive.app.dto.response.UsuarioDTO;
import com.eventhive.app.service.ServiceSeguidor;
import com.eventhive.app.service.ServiceUsuario;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuariosApiController {

    private final ServiceUsuario usuarioService;
    private final ServiceSeguidor seguidorService;

    //CONSULTAS
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<PagedResponse<UsuarioAdminDTO>>> listar(Pageable pageable) {
        Page<UsuarioAdminDTO> page = usuarioService.obtenerTodosAdmin(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Usuarios obtenidos", usuarioService.toPagedAdmin(page)));
    }

    @GetMapping("/buscar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<PagedResponse<UsuarioAdminDTO>>> filtrarUsuarios(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Long rolId,
            Pageable pageable) {

        Page<UsuarioAdminDTO> page = usuarioService.buscarAdminPorFiltros(nombre, rolId, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Resultados de búsqueda", usuarioService.toPagedAdmin(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<UsuarioAdminDTO>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Usuario obtenido", usuarioService.obtenerAdminPorId(id)));
    }

    @GetMapping("/misOrganizaciones-seguidas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PagedResponse<OrganizacionPublicaDTO>>> misOrganizacionesSeguidas(Pageable pageable) {
        Page<OrganizacionPublicaDTO> page = seguidorService.listarOrganizacionesSeguidas(pageable);

        PagedResponse<OrganizacionPublicaDTO> response = new PagedResponse<>(page.getContent(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());

        return ResponseEntity.ok(ApiResponse.ok("Organizaciones seguidas obtenidas", response));
    }

    //OPERACIONES PERFIL DE USUARIO
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
        return ResponseEntity.ok(ApiResponse.ok("Usuario eliminado exitosamente"));
    }

    @GetMapping("/perfil")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UsuarioDTO>> perfil() {
        return ResponseEntity.ok(ApiResponse.ok("Perfil obtenido",
                usuarioService.obtenerPerfil()));
    }

    // Actividad como comprador (entradas, eventos, favoritos, gasto): disponible para todos los roles
    @GetMapping("/perfil/actividad")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UsuarioActividadDTO>> miActividad() {
        return ResponseEntity.ok(ApiResponse.ok("Actividad obtenida", usuarioService.obtenerMiActividad()));
    }

    @PutMapping("/perfil")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UsuarioDTO>> actualizarPerfil(
            @Valid @RequestBody ActualizarPerfilRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Perfil actualizado", usuarioService.actualizarPerfil(request)));
    }

    @PutMapping("/perfil/cambiar-clave")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> cambiarClave(
            @Valid @RequestBody EditarClaveRequest datos) {
        usuarioService.cambiarClave(datos.getClaveActual(), datos.getClaveNueva());
        return ResponseEntity.ok(ApiResponse.ok("Contraseña actualizada correctamente"));
    }
}
