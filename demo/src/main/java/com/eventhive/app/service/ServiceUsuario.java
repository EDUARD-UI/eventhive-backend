package com.eventhive.app.service;

import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.response.OrganizacionDTO;
import com.eventhive.app.dto.response.UsuarioDTO;
import com.eventhive.app.dto.response.UsuarioSesionDTO;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.Rol;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.OrganizacionRepository;
import com.eventhive.app.repository.RolesRepository;
import com.eventhive.app.repository.UsuarioRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceUsuario {

    private final UsuarioRepository usuarioRepository;
    private final RolesRepository rolesRepository;
    private final OrganizacionRepository organizacionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticatedUserHelper authHelper;
    private final ServiceNotification serviceNotification;

    //consultas
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
    public Page<UsuarioDTO> obtenerModeradoresDTO(Pageable pageable) {
        return usuarioRepository.findByRolNombre("MODERADOR", pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<UsuarioDTO> obtenerOrganizaciones(Pageable pageable) {
        return usuarioRepository.findByRolNombre("ORGANIZACION", pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<OrganizacionDTO> obtenerTopOrganizaciones(Pageable pageable) {
        return organizacionRepository.findTopOrganizaciones(pageable).map(this::toOrganizacionDTO);
    }

    @Transactional(readOnly = true)
    public UsuarioSesionDTO obtenerSesionDTO(String correo) {
        return toSesionDTO(obtenerUsuarioPorCorreo(correo));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public UsuarioDTO obtenerPerfil() {
        return toDTO(authHelper.usuarioAutenticado());
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

        String nombreTrim = null;
        if (tieneNombre) {
            nombreTrim = nombre.trim();
        }

        if (tieneNombre && tieneRol) {
            return usuarioRepository.findByNombreYRolId(nombreTrim, rolId, pageable);
        }
        if (tieneNombre) {
            return usuarioRepository.findByNombreContieneIgnoreCase(nombreTrim, pageable);
        }
        if (tieneRol) {
            return usuarioRepository.findByRolId(rolId, pageable);
        }
        return usuarioRepository.findAll(pageable);
    }

    //operaciones crud
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public UsuarioDTO actualizarPerfil(UsuarioDTO dto) {
        Usuario u = authHelper.usuarioAutenticado();
        u.setNombreCompleto(dto.getNombre());
        u.setTelefono(dto.getTelefono());
        return toDTO(usuarioRepository.save(u));
    }

    @Transactional
    public Usuario asignarRol(Long usuarioId, Long rolId) {
        Usuario usuario = obtenerUsuarioPorId(usuarioId);
        Rol rol = rolesRepository.findById(rolId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no existe"));

        usuario.setRol(rol);
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario asignarModerador(Long usuarioId) {
        Rol moderador = rolesRepository.findByNombre("MODERADOR")
                .orElseThrow(() -> new ResourceNotFoundException("Rol MODERADOR no existe"));

        return asignarRol(usuarioId, moderador.getId());
    }

    @Transactional
    public Usuario revocarModerador(Long usuarioId) {
        Usuario usuario = obtenerUsuarioPorId(usuarioId);
        if (usuario.getRol() == null || !"MODERADOR".equalsIgnoreCase(usuario.getRol().getNombre())) {
            throw new BusinessException("El usuario no tiene rol MODERADOR");
        }

        Rol cliente = rolesRepository.findByNombre("CLIENTE")
                .orElseThrow(() -> new ResourceNotFoundException("Rol CLIENTE no existe"));

        usuario.setRol(cliente);
        Usuario actualizado = usuarioRepository.save(usuario);
        serviceNotification.notificarRevocacionRol(actualizado);
        return actualizado;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void crearUsuario(String nombre, String correo, String telefono, String clave, Long rolId) {
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new BusinessException("Correo ya registrado");
        }

        if (rolId == null) {
            throw new BusinessException("Debe especificar un rol");
        }

        Rol rol = rolesRepository.findById(rolId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no existe"));

        Usuario u = new Usuario();
        u.setNombreCompleto(nombre);
        u.setCorreo(correo);
        u.setTelefono(telefono);
        u.setClave(passwordEncoder.encode(clave));
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

    //cambio de clave
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

    // metodos auxiliares
    private UsuarioDTO toDTO(Usuario u) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(u.getId());
        dto.setNombre(u.getNombreCompleto());
        dto.setCorreo(u.getCorreo());
        dto.setTelefono(u.getTelefono());
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
        dto.setRolNombre(u.getRol() != null ? u.getRol().getNombre() : "");
        return dto;
    }

    public PagedResponse<UsuarioDTO> toPaged(Page<UsuarioDTO> page) {
        return new PagedResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    public PagedResponse<OrganizacionDTO> toPagedOrganizacion(Page<OrganizacionDTO> page) {
        return new PagedResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private OrganizacionDTO toOrganizacionDTO(Organizacion o) {
        OrganizacionDTO dto = new OrganizacionDTO();
        dto.setId(o.getId());
        dto.setRazonSocial(o.getRazonSocial());
        dto.setNit(o.getNit());
        dto.setRepresentanteLegal(o.getRepresentanteLegal());
        dto.setUrlRut(o.getUrlRut());
        dto.setFechaCreacion(o.getFechaCreacion());
        dto.setPromedioRating(o.getPromedioRating());
        dto.setTotalValoraciones(o.getTotalValoraciones());
        dto.setTotalSeguidores(o.getTotalSeguidores());
        dto.setTotalEventosCreados(o.getTotalEventosCreados());
        dto.setEventosFinalizados(o.getEventosFinalizados());
        dto.setEventosRechazados(o.getEventosRechazados());
        dto.setNivel(o.getNivel());
        return dto;
    }
}
