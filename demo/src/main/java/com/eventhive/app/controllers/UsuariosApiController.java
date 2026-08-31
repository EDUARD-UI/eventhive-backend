package com.eventhive.app.controllers;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.request.ActualizarPerfilRequest;
import com.eventhive.app.dto.request.EditarClaveRequest;
import com.eventhive.app.dto.response.OrganizacionDTO;
import com.eventhive.app.dto.response.UsuarioDTO;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.service.ServiceSeguidor;
import com.eventhive.app.service.ServiceUsuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuariosApiController {

    private final ServiceUsuario usuarioService;
    private final ServiceSeguidor seguidorService;

    //CONSULTAS
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<PagedResponse<UsuarioDTO>>> listar(Pageable pageable) {
        Page<UsuarioDTO> page = usuarioService.obtenerTodos(pageable)
                .map(usuarioService::toDTO);

        PagedResponse<UsuarioDTO> response = new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );

        return ResponseEntity.ok(ApiResponse.ok("Usuarios obtenidos", response));
    }

    @GetMapping("/buscar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<PagedResponse<UsuarioDTO>>> filtrarUsuarios(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Long rolId,
            Pageable pageable) {

        Page<UsuarioDTO> page = usuarioService.buscarPorFiltros(nombre, rolId, pageable)
                .map(usuarioService::toDTO);

        return ResponseEntity.ok(ApiResponse.ok("Resultados de búsqueda", usuarioService.toPaged(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<UsuarioDTO>> obtenerPorId(@PathVariable Long id) {
        Usuario usuario = usuarioService.obtenerUsuarioPorId(id);
        return ResponseEntity.ok(ApiResponse.ok("Usuario obtenido", usuarioService.toDTO(usuario)));
    }

    @GetMapping("/misOrganizaciones-seguidas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PagedResponse<OrganizacionDTO>>> misOrganizacionesSeguidas(Pageable pageable) {
        Page<OrganizacionDTO> page = seguidorService.listarOrganizacionesSeguidas(pageable);

        PagedResponse<OrganizacionDTO> response = new PagedResponse<>(page.getContent(), page.getNumber(),
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
