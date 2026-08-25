package com.eventhive.app.controllers;

import com.eventhive.app.dto.ApiResponse;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.request.EditarClaveRequest;
import com.eventhive.app.dto.response.OrganizacionDTO;
import com.eventhive.app.dto.response.UsuarioDTO;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.service.ServiceSeguidor;
import com.eventhive.app.service.ServiceUsuario;
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

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<PagedResponse<UsuarioDTO>>> listar(Pageable pageable) {
        Page<UsuarioDTO> page = usuarioService.obtenerTodos(pageable)
                .map(usuario -> {
                    UsuarioDTO dto = new UsuarioDTO();
                    dto.setId(usuario.getId());
                    dto.setNombre(usuario.getNombreCompleto());
                    dto.setCorreo(usuario.getCorreo());
                    dto.setTelefono(usuario.getTelefono());
                    if (usuario.getRol() != null) {
                        dto.setRolNombre(usuario.getRol().getNombre());
                    }
                    return dto;
                });

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
    public ResponseEntity<ApiResponse<PagedResponse<UsuarioDTO>>> buscar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Long rolId,
            Pageable pageable) {
        try {
            Page<Usuario> page = usuarioService.buscarPorFiltros(nombre, rolId, pageable);
            Page<UsuarioDTO> pageDto = page.map(usuario -> {
                UsuarioDTO dto = new UsuarioDTO();
                dto.setId(usuario.getId());
                dto.setNombre(usuario.getNombreCompleto());
                dto.setCorreo(usuario.getCorreo());
                dto.setTelefono(usuario.getTelefono());
                if (usuario.getRol() != null) {
                    dto.setRolNombre(usuario.getRol().getNombre());
                }
                return dto;
            });
            PagedResponse<UsuarioDTO> response = new PagedResponse<>(
                    pageDto.getContent(), pageDto.getNumber(), pageDto.getSize(),
                    pageDto.getTotalElements(), pageDto.getTotalPages()
            );
            return ResponseEntity.ok(ApiResponse.ok("Resultados de búsqueda", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error en búsqueda: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<UsuarioDTO>> obtenerPorId(@PathVariable Long id) {
        var usuario = usuarioService.obtenerUsuarioPorId(id);
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(usuario.getId());
        dto.setNombre(usuario.getNombreCompleto());
        dto.setCorreo(usuario.getCorreo());
        dto.setTelefono(usuario.getTelefono());
        if (usuario.getRol() != null) {
            dto.setRolNombre(usuario.getRol().getNombre());
        }
        return ResponseEntity.ok(ApiResponse.ok("Usuario obtenido", dto));
    }

    @GetMapping("/misOrganizaciones-seguidas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PagedResponse<Organizacion>>> misOrganizacionesSeguidas(Pageable pageable) {
        Page<Organizacion> page = seguidorService.listarOrganizacionesSeguidas(pageable);
        PagedResponse<Organizacion> response = new PagedResponse<>(page.getContent(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
        return ResponseEntity.ok(ApiResponse.ok("Organizaciones seguidas obtenidas", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
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
    public ResponseEntity<ApiResponse<UsuarioDTO>> actualizarPerfil(@RequestBody UsuarioDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok("Perfil actualizado",
                usuarioService.actualizarPerfil(dto)));
    }

    @PutMapping("/perfil/cambiar-clave")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> cambiarClave(
            @RequestBody EditarClaveRequest datos) {
        usuarioService.cambiarClave(datos.getClaveActual(), datos.getClaveNueva());
        return ResponseEntity.ok(ApiResponse.ok("Contraseña actualizada correctamente"));
    }
}
