package com.eventhive.app.service;

import com.eventhive.app.dto.request.PermisosOperadorRequest;
import com.eventhive.app.dto.response.OperadorDTO;
import com.eventhive.app.dto.response.OrganizacionDTO;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.*;
import com.eventhive.app.utils.AuthenticatedUserHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

// Solo expone datos de organizaciones ya APROBADAS
//se actualizan los datos cada q haya un cambio en las valoraciones, seguidores o eventos creados por la organizacion

@Service
@RequiredArgsConstructor
public class ServiceOrganizacion {

    private final UsuarioRepository usuarioRepository;
    private final OrganizacionRepository organizacionRepository;
    private final ValoracionRepository valoracionRepository;
    private final SeguidorRepository seguidorRepository;
    private final EventoRepository eventoRepository;
    private final RolesRepository rolesRepository;
    private final AuthenticatedUserHelper authHelper;

    //CONSULTAS
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public OrganizacionDTO obtenerPorId(Long organizacionId) {
        Organizacion organizacion = organizacionRepository.findById(organizacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Organización no encontrada"));
        return toDTO(organizacion);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('REPRESENTANTE')")
    public OrganizacionDTO miOrganizacion() {
        Usuario usuario = authHelper.usuarioAutenticado();
        if (usuario.getOrganizacion() == null)
            throw new BusinessException("Aún no tiene un perfil de organización aprobado");
        return toDTO(usuario.getOrganizacion());
    }

    // Lista los operadores de la organizacion
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('REPRESENTANTE')")
    public Page<OperadorDTO> listarOperadores(Pageable pageable) {
        Organizacion organizacion = organizacionDelRepresentante();
        return usuarioRepository.findByOrganizacionId(organizacion.getId(), pageable)
                .map(this::toOperadorDTO)
                .map(dto -> dto);
    }

    // GESTION DE PERMISOS Y OPERADORES
    @Transactional
    @PreAuthorize("hasRole('REPRESENTANTE')")
    public void actualizarPermisos(Long operadorId, PermisosOperadorRequest request) {
        Organizacion organizacion = organizacionDelRepresentante();
        Usuario operador = obtenerOperadorDeLaOrganizacion(operadorId, organizacion);

        operador.setPermisosEvento(Set.copyOf(request.getPermisos()));
        usuarioRepository.save(operador);
    }

    @Transactional
    @PreAuthorize("hasRole('REPRESENTANTE')")
    public void expulsarOperador(Long operadorId) {
        Organizacion organizacion = organizacionDelRepresentante();
        Usuario operador = obtenerOperadorDeLaOrganizacion(operadorId, organizacion);

        operador.setOrganizacion(null);
        operador.setPermisosEvento(Set.of());
        operador.setRol(rolesRepository.findByNombre("CLIENTE")
                .orElseThrow(() -> new ResourceNotFoundException("Rol CLIENTE no configurado")));
        usuarioRepository.save(operador);
    }

    //METRICAS DE LA ORGANIZACION
    @Transactional
    public void actualizarMetricasValoracion(Long organizacionId) {
        Organizacion organizacion = obtenerOrganizacion(organizacionId);

        int total = (int) valoracionRepository.countByOrganizacionId(organizacionId);
        double promedio = total > 0
                ? valoracionRepository.calcularPromedioByOrganizacionId(organizacionId)
                : 0.0;

        organizacion.setTotalValoraciones(total);
        organizacion.setPromedioRating(Math.round(promedio * 10.0) / 10.0);
        organizacionRepository.save(organizacion);
    }

    @Transactional
    public void actualizarTotalSeguidores(Long organizacionId) {
        Organizacion organizacion = obtenerOrganizacion(organizacionId);
        int total = (int) seguidorRepository.countByOrganizacionId(organizacionId);
        organizacion.setTotalSeguidores(total);
        organizacionRepository.save(organizacion);
    }

    @Transactional
    public void actualizarTotalEventos(Long organizacionId) {
        Organizacion organizacion = obtenerOrganizacion(organizacionId);
        int total = (int) eventoRepository.countByOrganizacionId(organizacionId);
        organizacion.setTotalEventosCreados(total);
        organizacionRepository.save(organizacion);
    }

    //METODOS AUXILIARES Y DE MAPEO
    private Organizacion organizacionDelRepresentante() {
        Usuario representante = authHelper.usuarioAutenticado();
        Organizacion organizacion = representante.getOrganizacion();
        if (organizacion == null || !organizacion.getRepresentante().getId().equals(representante.getId()))
            throw new BusinessException("No tienes una organización para administrar");
        return organizacion;
    }

    private Usuario obtenerOperadorDeLaOrganizacion(Long operadorId, Organizacion organizacion) {
        Usuario operador = usuarioRepository.findById(operadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operador no encontrado"));

        boolean perteneceAOrganizacion = operador.getOrganizacion() != null
                && operador.getOrganizacion().getId().equals(organizacion.getId());
        boolean esRepresentante = organizacion.getRepresentante().getId().equals(operador.getId());

        if (!perteneceAOrganizacion || esRepresentante)
            throw new BusinessException("El usuario indicado no es un operador de tu organización");

        return operador;
    }

    // Obtiene la Organizacion asociada a un usuario
    private Organizacion obtenerOrganizacion(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (usuario.getOrganizacion() == null)
            throw new BusinessException("El usuario no se encuentra asociado a una organización verificada");
        return usuario.getOrganizacion();
    }

    private OrganizacionDTO toDTO(Organizacion o) {
        OrganizacionDTO dto = new OrganizacionDTO();
        dto.setId(o.getId());
        dto.setRepresentante(o.getRepresentante().getNombreCompleto());
        dto.setRazonSocial(o.getRazonSocial());
        dto.setNit(o.getNit());
        dto.setCorreoContacto(o.getCorreoContacto());
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

    private OperadorDTO toOperadorDTO(Usuario u) {
        OperadorDTO dto = new OperadorDTO();
        dto.setId(u.getId());
        dto.setNombreCompleto(u.getNombreCompleto());
        dto.setCorreo(u.getCorreo());
        dto.setPermisosEvento(u.getPermisosEvento());
        return dto;
    }
}
