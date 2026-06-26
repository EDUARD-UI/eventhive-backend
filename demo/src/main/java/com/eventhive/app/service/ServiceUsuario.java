package com.eventhive.app.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.dto.UsuarioDTO;
import com.eventhive.app.dto.UsuarioSesionDTO;
import com.eventhive.app.enums.NivelUsuario;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Rol;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.RolesRepository;
import com.eventhive.app.repository.UsuarioRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceUsuario {

    private final UsuarioRepository usuarioRepository;
    private final RolesRepository rolesRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticatedUserHelper authHelper;
    private final ServiceFidelizacion serviceFidelizacion;
    @Transactional(readOnly = true)
    public Usuario obtenerUsuarioPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    @Transactional(readOnly = true)
    public Usuario obtenerUsuarioPorCorreo(String correo) {
        return usuarioRepository.findByCorreoConRol(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Page<Usuario> obtenerTodos(Pageable pageable) {
        return usuarioRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Page<Usuario> buscarPorFiltros(String nombre, Long rolId, Pageable pageable) {
        boolean tieneNombre = nombre != null && !nombre.isBlank();
        boolean tieneRol = rolId != null;
        if (tieneNombre && tieneRol) {
            return usuarioRepository.findByNombreYRolId(nombre.trim(), rolId, pageable);
        }
        if (tieneNombre) {
            return usuarioRepository.findByNombreContieneIgnoreCase(nombre.trim(), pageable);
        }
        if (tieneRol) {
            return usuarioRepository.findByRolId(rolId, pageable);
        }
        return usuarioRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public UsuarioSesionDTO obtenerSesionDTO(String correo) {
        Usuario u = obtenerUsuarioPorCorreo(correo);
        return toSesionDTO(u);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public UsuarioDTO obtenerPerfil() {
        return toDTO(authHelper.usuarioAutenticado());
    }

    @Transactional
    public boolean actualizarNivelSiCambia(Usuario usuario) {
        NivelUsuario nivelAnterior = usuario.getNivel();
        NivelUsuario nivelNuevo = serviceFidelizacion.calcularNivel(usuario.getCantidadCompras());

        if (nivelNuevo != nivelAnterior) {
            usuario.setNivel(nivelNuevo);
            usuarioRepository.save(usuario);
            return true; // indica que hubo cambio de nivel
        }

        usuarioRepository.save(usuario);
        return false;
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public UsuarioDTO actualizarPerfil(UsuarioDTO dto) {
        Usuario u = authHelper.usuarioAutenticado();
        u.setNombreCompleto(dto.getNombre());
        u.setTelefono(dto.getTelefono());
        return toDTO(usuarioRepository.save(u));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void crearUsuario(String nombre, String correo,
            String telefono, String clave, Long rolId) {
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new BusinessException("Correo ya registrado");
        }

        Rol rol = rolesRepository.findById(rolId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no existe"));

        Usuario u = new Usuario();
        u.setNombreCompleto(nombre);
        u.setCorreo(correo);
        u.setTelefono(telefono);
        u.setClave(passwordEncoder.encode(clave));
        u.setRol(rol);
        u.setEsVerificado(false);
        usuarioRepository.save(u);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void actualizarUsuario(Long id, String nombre, String telefono) {
        Usuario u = obtenerUsuarioPorId(id);
        u.setNombreCompleto(nombre);
        u.setTelefono(telefono);
        usuarioRepository.save(u);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void asignarRol(Long usuarioId, Long rolId) {
        Usuario u = obtenerUsuarioPorId(usuarioId);
        Rol rol = rolesRepository.findById(rolId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no existe"));
        u.setRol(rol);
        usuarioRepository.save(u);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void eliminarUsuario(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuario no encontrado");
        }
        usuarioRepository.deleteById(id);
    }

    // Valida la clave actual y la reemplaza con la nueva cifrada
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void cambiarClave(String claveActual, String claveNueva) {
        Usuario u = authHelper.usuarioAutenticado();

        if (!passwordEncoder.matches(claveActual, u.getClave())) {
            throw new BusinessException("La contraseña actual es incorrecta");
        }

        if (claveNueva == null || claveNueva.length() < 8) {
            throw new BusinessException("La nueva contraseña debe tener al menos 8 caracteres");
        }

        u.setClave(passwordEncoder.encode(claveNueva));
        usuarioRepository.save(u);
    }

    // --- helpers privados ---
    private UsuarioDTO toDTO(Usuario u) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(u.getId());
        dto.setNombre(u.getNombreCompleto());
        dto.setCorreo(u.getCorreo());
        dto.setTelefono(u.getTelefono());
        dto.setEsVerificado(u.getEsVerificado() != null && u.getEsVerificado());
        if (u.getRol() != null) {
            dto.setRolNombre(u.getRol().getNombre());
        }
        return dto;
    }

    private UsuarioSesionDTO toSesionDTO(Usuario u) {
        UsuarioSesionDTO dto = new UsuarioSesionDTO();
        dto.setId(u.getId());
        dto.setNombre(u.getNombreCompleto());
        dto.setCorreo(u.getCorreo());
        dto.setTelefono(u.getTelefono());
        dto.setEsVerificado(u.getEsVerificado() != null && u.getEsVerificado());
        dto.setRolNombre(u.getRol() != null ? u.getRol().getNombre() : "");
        dto.setNivel(u.getNivel());
        dto.setCantidadCompras(u.getCantidadCompras());
        return dto;
    }
}
