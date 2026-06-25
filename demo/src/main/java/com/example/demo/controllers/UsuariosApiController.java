package com.example.demo.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.PagedResponse;
import com.example.demo.dto.UsuarioDTO;
import com.example.demo.model.Usuario;
import com.example.demo.service.ServiceUsuario;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuariosApiController {

    private final ServiceUsuario usuarioService;

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

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> crear(
            @RequestParam String nombre,
            @RequestParam String correo,
            @RequestParam String telefono,
            @RequestParam String clave,
            @RequestParam Long rolId) {

        usuarioService.crearUsuario(nombre, correo, telefono, clave, rolId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Usuario creado exitosamente"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> actualizar(
            @PathVariable Long id,
            @RequestParam String nombre,
            @RequestParam String telefono) {

        usuarioService.actualizarUsuario(id, nombre, telefono);
        return ResponseEntity.ok(ApiResponse.ok("Usuario actualizado exitosamente"));
    }

    @PutMapping("/{id}/asignar-rol")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> asignarRol(
            @PathVariable Long id,
            @RequestParam Long rolId) {

        usuarioService.asignarRol(id, rolId);
        return ResponseEntity.ok(ApiResponse.ok("Rol asignado exitosamente"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or isAuthenticated()")
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
            @RequestParam String claveActual,
            @RequestParam String claveNueva) {
        usuarioService.cambiarClave(claveActual, claveNueva);
        return ResponseEntity.ok(ApiResponse.ok("Contraseña actualizada correctamente"));
    }
}
