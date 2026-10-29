package com.eventhive.app.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<PagedResponse<UsuarioAdminDTO>>> listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Long rolId,
            @RequestParam(required = false) String rol,
            Pageable pageable) {
        Page<UsuarioAdminDTO> page;
        if ((nombre != null && !nombre.isBlank()) || rolId != null || (rol != null && !rol.isBlank())) {
            page = usuarioService.buscarAdminPorFiltros(nombre, rolId, rol, pageable);
        } else {
            page = usuarioService.obtenerTodosAdmin(pageable);
        }
        return ResponseEntity.ok(ApiResponse.ok("Usuarios obtenidos", usuarioService.toPagedAdmin(page)));
    }

    @GetMapping("/buscar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<PagedResponse<UsuarioAdminDTO>>> filtrarUsuarios(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Long rolId,
            @RequestParam(required = false) String rol,
            Pageable pageable) {

        Page<UsuarioAdminDTO> page = usuarioService.buscarAdminPorFiltros(nombre, rolId, rol, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Resultados de búsqueda", usuarioService.toPagedAdmin(page)));
    }

    @GetMapping({"/trabajadores/marketing", "/marketing"})
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<java.util.List<UsuarioAdminDTO>>> listarTrabajadoresMarketing() {
        return ResponseEntity.ok(ApiResponse.ok("Trabajadores de marketing obtenidos",
                usuarioService.listarTrabajadoresMarketing()));
    }

    @RequestMapping(value = {"/{id}/revocar-marketing", "/{id}/expulsar-marketing"},
            method = {RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.POST})
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> revocarRolMarketing(@PathVariable Long id) {
        usuarioService.revocarMarketing(id);
        return ResponseEntity.ok(ApiResponse.ok("Rol de marketing revocado exitosamente"));
    }

    @RequestMapping(value = {"/{id}/revocar-moderador", "/{id}/expulsar-moderador"},
            method = {RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.POST})
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> revocarRolModerador(@PathVariable Long id) {
        usuarioService.revocarModerador(id);
        return ResponseEntity.ok(ApiResponse.ok("Rol de moderador revocado exitosamente"));
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

    @RequestMapping(value = {"/perfil/imagen", "/perfil/foto"}, method = {RequestMethod.PUT, RequestMethod.POST}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UsuarioDTO>> actualizarImagenPerfil(
            @RequestParam(value = "imagen", required = false) MultipartFile imagen,
            @RequestParam(value = "foto", required = false) MultipartFile foto,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar) {
        MultipartFile archivoFinal = (imagen != null && !imagen.isEmpty()) ? imagen
                : (foto != null && !foto.isEmpty() ? foto
                : (file != null && !file.isEmpty() ? file : avatar));

        if (archivoFinal == null || archivoFinal.isEmpty()) {
            throw new com.eventhive.app.exception.BusinessException("Debe proporcionar un archivo de imagen (parámetros aceptados: 'imagen', 'foto', 'file' o 'avatar')");
        }

        return ResponseEntity.ok(ApiResponse.ok("Imagen de perfil actualizada",
                usuarioService.actualizarPerfil(null, archivoFinal)));
    }

    @PutMapping("/perfil/cambiar-clave")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> cambiarClave(
            @Valid @RequestBody EditarClaveRequest datos) {
        usuarioService.cambiarClave(datos.getClaveActual(), datos.getClaveNueva());
        return ResponseEntity.ok(ApiResponse.ok("Contraseña actualizada correctamente"));
    }
}
