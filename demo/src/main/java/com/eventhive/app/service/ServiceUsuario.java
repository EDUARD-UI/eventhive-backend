package com.eventhive.app.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.request.ActualizarPerfilRequest;
import com.eventhive.app.dto.response.OrganizacionPublicaDTO;
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

@Service
@RequiredArgsConstructor
public class ServiceUsuario {

    private final UsuarioRepository usuarioRepository;
    private final RolesRepository rolesRepository;
    private final OrganizacionRepository organizacionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticatedUserHelper authHelper;
    private final ServiceNotification serviceNotification;

    //CONSULTAS Y FILTROS
    public Usuario obtenerUsuarioPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    public Page<UsuarioDTO> obtenerModeradoresDTO(Pageable pageable) {
        return usuarioRepository.findByRolNombre("MODERADOR", pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<OrganizacionPublicaDTO> obtenerOrganizaciones(Pageable pageable) {
        return organizacionRepository.findAll(pageable).map(this::toOrganizacionPublicaDTO);
    }

    @Transactional(readOnly = true)
    public Page<OrganizacionPublicaDTO> obtenerTopOrganizaciones(Pageable pageable) {
        return organizacionRepository.findTopOrganizaciones(pageable).map(this::toOrganizacionPublicaDTO);
    }

    @Transactional(readOnly = true)
    public UsuarioSesionDTO obtenerSesionDTO(String correo) {
        return toSesionDTO(obtenerUsuarioPorCorreo(correo));
    }

    @Transactional(readOnly = true)
    public UsuarioDTO obtenerPerfil() {
        return toDTO(authHelper.usuarioAutenticado());
    }

    @Transactional(readOnly = true)
    public Page<Usuario> obtenerTodos(Pageable pageable) {
        return usuarioRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
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

    //OPERACIONES CRUD
    @Transactional
    public UsuarioDTO actualizarPerfil(ActualizarPerfilRequest request) {
        Usuario u = authHelper.usuarioAutenticado();
        u.setNombreCompleto(request.getNombre());
        u.setTelefono(request.getTelefono());
        return toDTO(usuarioRepository.save(u));
    }

    @Transactional
    public void eliminarUsuario(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuario no encontrado");
        }
        usuarioRepository.deleteById(id);
    }

    @Transactional
    public void asignarModerador(Long usuarioId) {
        Usuario usuario = obtenerUsuarioPorId(usuarioId);

        if (usuario.getRol() == null || !"CLIENTE".equalsIgnoreCase(usuario.getRol().getNombre())) {
            throw new BusinessException("Solo se puede asignar MODERADOR a usuarios con rol CLIENTE");
        }
        if (usuario.getOrganizacion() != null) {
            throw new BusinessException("No se puede asignar MODERADOR a un usuario vinculado a una organización");
        }

        Rol moderador = rolesRepository.findByNombre("MODERADOR")
                .orElseThrow(() -> new ResourceNotFoundException("Rol MODERADOR no existe"));

        usuario.setRol(moderador);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void revocarModerador(Long usuarioId) {
        Usuario usuario = obtenerUsuarioPorId(usuarioId);
        if (usuario.getRol() == null || !"MODERADOR".equalsIgnoreCase(usuario.getRol().getNombre())) {
            throw new BusinessException("El usuario no tiene rol MODERADOR");
        }

        Rol cliente = rolesRepository.findByNombre("CLIENTE")
                .orElseThrow(() -> new ResourceNotFoundException("Rol CLIENTE no existe"));

        usuario.setRol(cliente);
        Usuario actualizado = usuarioRepository.save(usuario);
        serviceNotification.notificarRevocacionRol(actualizado);
    }

    //cambio de clave
    @Transactional
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

    // METODOS AUXILIARES Y MAPEO
    private Usuario obtenerUsuarioPorCorreo(String correo) {
        return usuarioRepository.findByCorreoConRol(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    public UsuarioDTO toDTO(Usuario u) {
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

    public PagedResponse<OrganizacionPublicaDTO> toPagedOrganizacion(Page<OrganizacionPublicaDTO> page) {
        return new PagedResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private OrganizacionPublicaDTO toOrganizacionPublicaDTO(Organizacion o) {
        OrganizacionPublicaDTO dto = new OrganizacionPublicaDTO();
        dto.setId(o.getId());
        dto.setRazonSocial(o.getRazonSocial());
        dto.setRepresentante(o.getRepresentante().getNombreCompleto());
        dto.setFechaCreacion(o.getFechaCreacion());
        dto.setPromedioRating(o.getPromedioRating());
        dto.setTotalValoraciones(o.getTotalValoraciones());
        dto.setTotalSeguidores(o.getTotalSeguidores());
        dto.setTotalEventosCreados(o.getTotalEventosCreados());
        dto.setNivel(o.getNivel());
        return dto;
    }
}
