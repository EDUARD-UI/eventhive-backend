package com.eventhive.app.service;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.request.ActualizarPerfilRequest;
import com.eventhive.app.dto.response.OrganizacionPublicaDTO;
import com.eventhive.app.dto.response.UsuarioActividadDTO;
import com.eventhive.app.dto.response.UsuarioAdminDTO;
import com.eventhive.app.dto.response.UsuarioDTO;
import com.eventhive.app.dto.response.UsuarioSesionDTO;
import com.eventhive.app.enums.EstadoCompra;
import com.eventhive.app.enums.EstadoOrganizacion;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.Rol;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.CompraRepository;
import com.eventhive.app.repository.ItemCompraRepository;
import com.eventhive.app.repository.ListaDeseoRepository;
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
    private final CompraRepository compraRepository;
    private final ItemCompraRepository itemCompraRepository;
    private final ListaDeseoRepository listaDeseoRepository;
    private final SupabaseStorageService storageService;
    private final com.eventhive.app.config.SupabaseStorageConfig storageConfig;

    //CONSULTAS Y FILTROS
    @Transactional(readOnly = true)
    public Usuario obtenerUsuarioPorId(Long id) {
        return usuarioRepository.findByIdConRol(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }

    @Transactional(readOnly = true)
    public Page<UsuarioDTO> obtenerModeradoresDTO(Pageable pageable) {
        return usuarioRepository.findByRolNombreConRol("MODERADOR", pageable).map(this::toDTO);
    }

    // Listados para el panel administrativo (rol + organización cargados con JOIN FETCH)
    @Transactional(readOnly = true)
    public Page<UsuarioAdminDTO> obtenerTodosAdmin(Pageable pageable) {
        return usuarioRepository.findAllConRol(pageable).map(this::toAdminDTO);
    }

    @Transactional(readOnly = true)
    public Page<UsuarioAdminDTO> buscarAdminPorFiltros(String nombre, Long rolId, Pageable pageable) {
        return buscarPorFiltros(nombre, rolId, pageable).map(this::toAdminDTO);
    }

    @Transactional(readOnly = true)
    public UsuarioAdminDTO obtenerAdminPorId(Long id) {
        return toAdminDTO(obtenerUsuarioPorId(id));
    }

    // estado == null -> sin filtro (comportamiento previo)
    @Transactional(readOnly = true)
    public Page<OrganizacionPublicaDTO> obtenerOrganizaciones(EstadoOrganizacion estado, Pageable pageable) {
        Page<Organizacion> page = estado == null
                ? organizacionRepository.findAllConRepresentante(pageable)
                : organizacionRepository.findByEstadoConRepresentante(estado, pageable);
        return page.map(this::toOrganizacionPublicaDTO);
    }

    @Transactional(readOnly = true)
    public Page<OrganizacionPublicaDTO> buscarOrganizacionesPorRazonSocial(String razonSocial,
                                                                            EstadoOrganizacion estado,
                                                                            Pageable pageable) {
        String texto = razonSocial.trim();
        Page<Organizacion> page = estado == null
                ? organizacionRepository.findByRazonSocial(texto, pageable)
                : organizacionRepository.findByRazonSocialAndEstado(texto, estado, pageable);
        return page.map(this::toOrganizacionPublicaDTO);
    }

    @Transactional(readOnly = true)
    public Page<OrganizacionPublicaDTO> obtenerTopOrganizaciones(EstadoOrganizacion estado, Pageable pageable) {
        Page<Organizacion> page = estado == null
                ? organizacionRepository.findTopOrganizaciones(pageable)
                : organizacionRepository.findTopByEstado(estado, pageable);
        return page.map(this::toOrganizacionPublicaDTO);
    }

    @Transactional(readOnly = true)
    public UsuarioSesionDTO obtenerSesionDTO(String correo) {
        return toSesionDTO(obtenerUsuarioPorCorreo(correo));
    }

    @Transactional(readOnly = true)
    public UsuarioDTO obtenerPerfil() {
        return toDTO(authHelper.usuarioAutenticado());
    }

    // Resumen de la actividad como comprador del usuario autenticado (sin restricción por rol)
    @Transactional(readOnly = true)
    public UsuarioActividadDTO obtenerMiActividad() {
        Long usuarioId = authHelper.usuarioAutenticado().getId();
        return new UsuarioActividadDTO(
                compraRepository.countByClienteIdAndEstado(usuarioId, EstadoCompra.CONFIRMADA),
                Objects.requireNonNullElse(itemCompraRepository.contarEntradasCompradas(usuarioId), 0L),
                itemCompraRepository.contarEventosComprados(usuarioId),
                listaDeseoRepository.countByUsuarioId(usuarioId),
                Objects.requireNonNullElse(
                        compraRepository.sumarTotalPorClienteYEstado(usuarioId, EstadoCompra.CONFIRMADA),
                        BigDecimal.ZERO));
    }

    @Transactional(readOnly = true)
    public Page<Usuario> obtenerTodos(Pageable pageable) {
        return usuarioRepository.findAllConRol(pageable);
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
        return usuarioRepository.findAllConRol(pageable);
    }

    //OPERACIONES CRUD
    @Transactional
    public UsuarioDTO actualizarPerfil(ActualizarPerfilRequest request) {
        return actualizarPerfil(request, null);
    }

    @Transactional
    public UsuarioDTO actualizarPerfil(ActualizarPerfilRequest request, org.springframework.web.multipart.MultipartFile imagen) {
        Usuario u = authHelper.usuarioAutenticado();
        if (request != null) {
            if (request.getNombre() != null && !request.getNombre().isBlank()) {
                u.setNombreCompleto(request.getNombre().trim());
            }
            if (request.getTelefono() != null) {
                u.setTelefono(request.getTelefono().trim());
            }
        }
        if (imagen != null && !imagen.isEmpty()) {
            if (u.getImagenPerfil() != null && !u.getImagenPerfil().isBlank()) {
                storageService.eliminarImagenDeBucket(storageConfig.getBucketImagenPerfil(), u.getImagenPerfil());
            }
            String url = storageService.subirImagenPerfilUsuario(imagen);
            u.setImagenPerfil(url);
        }
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
        dto.setImagenPerfil(u.getImagenPerfil());
        if (u.getRol() != null) {
            dto.setRolNombre(u.getRol().getNombre());
        }
        return dto;
    }

    public UsuarioAdminDTO toAdminDTO(Usuario u) {
        UsuarioAdminDTO dto = new UsuarioAdminDTO();
        dto.setId(u.getId());
        dto.setNombre(u.getNombreCompleto());
        dto.setTelefono(u.getTelefono());
        dto.setImagenPerfil(u.getImagenPerfil());
        dto.setRolNombre(u.getRol() != null ? u.getRol().getNombre() : null);
        if (u.getOrganizacion() != null) {
            dto.setOrganizacion(new UsuarioAdminDTO.OrganizacionResumenDTO(
                    u.getOrganizacion().getId(), u.getOrganizacion().getRazonSocial()));
        }
        return dto;
    }

    private UsuarioSesionDTO toSesionDTO(Usuario u) {
        UsuarioSesionDTO dto = new UsuarioSesionDTO();
        dto.setId(u.getId());
        dto.setNombre(u.getNombreCompleto());
        dto.setCorreo(u.getCorreo());
        dto.setTelefono(u.getTelefono());
        dto.setImagenPerfil(u.getImagenPerfil());
        dto.setRolNombre(u.getRol() != null ? u.getRol().getNombre() : "");
        return dto;
    }

    public PagedResponse<UsuarioDTO> toPaged(Page<UsuarioDTO> page) {
        return new PagedResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    public PagedResponse<UsuarioAdminDTO> toPagedAdmin(Page<UsuarioAdminDTO> page) {
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
        dto.setRepresentante(o.getRepresentante() != null ? o.getRepresentante().getNombreCompleto() : null);
        dto.setDescripcion(o.getDescripcion());
        dto.setUrlLogo(o.getUrlLogo());
        dto.setFechaCreacion(o.getFechaCreacion());
        dto.setPromedioRating(o.getPromedioRating());
        dto.setTotalValoraciones(o.getTotalValoraciones());
        dto.setTotalSeguidores(o.getTotalSeguidores());
        dto.setTotalEventosCreados(o.getTotalEventosCreados());
        dto.setNivel(o.getNivel());
        dto.setEstado(o.getEstado());
        return dto;
    }
}