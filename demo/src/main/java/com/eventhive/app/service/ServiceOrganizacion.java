package com.eventhive.app.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.eventhive.app.config.SupabaseStorageConfig;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.request.ActualizarOrganizacionRequest;
import com.eventhive.app.dto.request.PermisosOperadorRequest;
import com.eventhive.app.dto.response.EventoEntradasResumenDTO;
import com.eventhive.app.dto.response.EventosPorCategoriaDTO;
import com.eventhive.app.dto.response.LocalidadEntradasDTO;
import com.eventhive.app.dto.response.OperadorDTO;
import com.eventhive.app.dto.response.OrganizacionDTO;
import com.eventhive.app.dto.response.OrganizacionEstadisticasDTO;
import com.eventhive.app.dto.response.PanelEntradasDTO;
import com.eventhive.app.dto.response.RutUrlDTO;
import com.eventhive.app.dto.response.VentasOrganizacionResumenDTO;
import com.eventhive.app.enums.EstadoOrganizacion;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Localidad;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.CategoriaRepository;
import com.eventhive.app.repository.CompraRepository;
import com.eventhive.app.repository.EventoRepository;
import com.eventhive.app.repository.ItemCompraRepository;
import com.eventhive.app.repository.LocalidadRepository;
import com.eventhive.app.repository.OrganizacionRepository;
import com.eventhive.app.repository.RolesRepository;
import com.eventhive.app.repository.SeguidorRepository;
import com.eventhive.app.repository.UsuarioRepository;
import com.eventhive.app.repository.ValoracionRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Solo expone datos de organizaciones ya APROBADAS
//se actualizan los datos cada q haya un cambio en las valoraciones, seguidores o eventos creados por la organizacion

@Slf4j
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
    private final SupabaseStorageService storageService;
    private final SupabaseStorageConfig storageConfig;
    private final ServiceNotification serviceNotification;
    private final CompraRepository compraRepository;
    private final ItemCompraRepository itemCompraRepository;
    private final LocalidadRepository localidadRepository;
    private final CategoriaRepository categoriaRepository;
    private final com.eventhive.app.repository.SolicitudVerificacionRepository solicitudRepository;

    //CONSULTAS
    @Transactional(readOnly = true)
    public OrganizacionDTO obtenerPorId(Long organizacionId) {
        Organizacion organizacion = organizacionRepository.findByIdConRepresentante(organizacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Organización no encontrada"));
        return toDTO(organizacion);
    }

    @Transactional(readOnly = true)
    public OrganizacionDTO miOrganizacion() {
        Usuario usuario = authHelper.usuarioAutenticado();
        Organizacion organizacion = usuario.getOrganizacion();
        if (organizacion == null) {
            organizacion = organizacionRepository.findByRepresentanteIdConRepresentante(usuario.getId())
                    .orElse(null);
        }
        if (organizacion == null) {
            throw new ResourceNotFoundException("No tienes un perfil de organización asociado");
        }
        Organizacion orgCompleta = organizacionRepository.findByIdConRepresentante(organizacion.getId())
                .orElse(organizacion);
        return toDTO(orgCompleta);
    }

    @Transactional(readOnly = true)
    public RutUrlDTO obtenerUrlRut(Long organizacionId) {
        Usuario usuario = authHelper.usuarioAutenticado();
        Organizacion organizacion = organizacionRepository.findById(organizacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Organización no encontrada"));

        boolean esAdministrador = usuario.getRol() != null
                && ("ADMINISTRADOR".equalsIgnoreCase(usuario.getRol().getNombre())
                || "MODERADOR".equalsIgnoreCase(usuario.getRol().getNombre()));
        boolean esRepresentante = usuario.getOrganizacion() != null
            && usuario.getOrganizacion().getId().equals(organizacionId)
            && usuario.getOrganizacion().getRepresentante() != null
            && usuario.getOrganizacion().getRepresentante().getId().equals(usuario.getId());
        if (!esAdministrador && !esRepresentante) {
            throw new BusinessException("No autorizado para consultar este RUT");
        }
        if (organizacion.getUrlRut() == null || organizacion.getUrlRut().isBlank()) {
            throw new BusinessException("La organización no tiene un RUT almacenado");
        }

        long expiresInSeconds = 3600L;
        return new RutUrlDTO(storageService.generarUrlFirmada(organizacion.getUrlRut(), expiresInSeconds),
                expiresInSeconds);
    }

    // Lista los trabajadores de la organizacion
    @Transactional(readOnly = true)
    public Page<OperadorDTO> listarOperadores(Pageable pageable) {
        Organizacion organizacion = organizacionDelRepresentante();
        return usuarioRepository
                .findOperadoresByOrganizacionId(organizacion.getId(), pageable)
                .map(this::toOperadorDTO);
    }

    // GESTION DE PERMISOS Y OPERADORES
    @Transactional
    public OperadorDTO actualizarPermisos(Long operadorId, PermisosOperadorRequest request) {
        Organizacion organizacion = organizacionDelRepresentante();
        Usuario operador = obtenerOperadorDeLaOrganizacion(operadorId, organizacion);

        Set<com.eventhive.app.enums.PermisoEvento> permisos = (request != null && request.getPermisos() != null)
                ? request.getPermisos().stream().filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet())
                : Set.of();

        if (operador.getPermisosEvento() == null) {
            operador.setPermisosEvento(new HashSet<>());
        } else {
            operador.getPermisosEvento().clear();
        }
        operador.getPermisosEvento().addAll(permisos);
        Usuario guardado = usuarioRepository.saveAndFlush(operador);
        return toOperadorDTO(guardado);
    }

    @Transactional
    public void expulsarOperador(Long operadorId) {
        Organizacion organizacion = organizacionDelRepresentante();
        Usuario operador = obtenerOperadorDeLaOrganizacion(operadorId, organizacion);

        operador.setOrganizacion(null);
        if (operador.getPermisosEvento() != null) {
            operador.getPermisosEvento().clear();
        } else {
            operador.setPermisosEvento(new HashSet<>());
        }
        operador.setRol(rolesRepository.findByNombre("CLIENTE")
                .orElseThrow(() -> new ResourceNotFoundException("Rol CLIENTE no configurado")));
        usuarioRepository.saveAndFlush(operador);

        serviceNotification.notificarRevocacionOperador(operador, organizacion.getRazonSocial());
    }

    //METRICAS DE LA ORGANIZACION
    @Transactional
    public void actualizarMetricasValoracion(Long organizacionId) {
        Organizacion organizacion = buscarOrganizacionPorId(organizacionId);

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
        Organizacion organizacion = buscarOrganizacionPorId(organizacionId);
        int total = (int) seguidorRepository.countByOrganizacionId(organizacionId);
        organizacion.setTotalSeguidores(total);
        organizacionRepository.save(organizacion);
    }

    @Transactional
    public void actualizarTotalEventos(Long organizacionId) {
        Organizacion organizacion = buscarOrganizacionPorId(organizacionId);
        int total = (int) eventoRepository.countByOrganizacionId(organizacionId);
        organizacion.setTotalEventosCreados(total);
        organizacionRepository.save(organizacion);
    }

    // CAMBIO DE ESTADO (único punto de entrada: valida la transición)
    @Transactional
    public void cambiarEstado(Organizacion organizacion, EstadoOrganizacion nuevoEstado) {
        EstadoOrganizacion actual = organizacion.getEstado();
        if (actual == nuevoEstado) return; // idempotente
        if (actual == null || !actual.puedeTransicionarA(nuevoEstado)) {
            throw new BusinessException("Transición de estado no permitida: " + actual + " -> " + nuevoEstado);
        }
        organizacion.setEstado(nuevoEstado);
        organizacionRepository.save(organizacion);
    }

    //METODOS AUXILIARES Y DE MAPEO
    private Organizacion organizacionDelRepresentante() {
        Usuario representante = authHelper.usuarioAutenticado();
        Organizacion organizacion = representante.getOrganizacion();
        if (organizacion == null) {
            organizacion = organizacionRepository.findByRepresentanteIdConRepresentante(representante.getId())
                    .orElse(null);
        }
        if (organizacion == null
                || organizacion.getRepresentante() == null
                || !organizacion.getRepresentante().getId().equals(representante.getId())
                || representante.getRol() == null
                || !"REPRESENTANTE".equalsIgnoreCase(representante.getRol().getNombre())) {
            throw new BusinessException("No tienes una organización para administrar");
        }
        return organizacion;
    }

    private Usuario obtenerOperadorDeLaOrganizacion(Long operadorId, Organizacion organizacion) {
        Usuario operador = usuarioRepository.findById(operadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        boolean perteneceAOrganizacion = operador.getOrganizacion() != null
                && operador.getOrganizacion().getId().equals(organizacion.getId());

        boolean esOperador = operador.getRol() != null
                && "OPERADOR".equalsIgnoreCase(operador.getRol().getNombre());

        if (!perteneceAOrganizacion || !esOperador) {
            throw new BusinessException(
                    "El usuario indicado no pertenece a tu organización");
        }

        return operador;
    }

    private Organizacion buscarOrganizacionPorId(Long organizacionId) {
        return organizacionRepository.findById(organizacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Organización no encontrada: " + organizacionId));
    }

    private Organizacion organizacionDelUsuario() {
        Usuario usuario = authHelper.usuarioAutenticado();
        Organizacion organizacion = usuario.getOrganizacion();
        if (organizacion == null) {
            organizacion = organizacionRepository.findByRepresentanteIdConRepresentante(usuario.getId())
                    .orElse(null);
        }
        if (organizacion == null) {
            throw new BusinessException("El usuario no pertenece a ninguna organización");
        }
        return organizacion;
    }

    @Transactional(readOnly = true)
    public OrganizacionEstadisticasDTO obtenerEstadisticasMiOrganizacion() {
        Organizacion org = organizacionDelUsuario();
        Long orgId = org.getId();

        long eventosActivos = eventoRepository.countActivosByOrganizacionId(orgId);
        long boletasVendidas = itemCompraRepository.contarBoletasVendidasEventosPublicadosPorOrganizacion(orgId);
        long totalEventos = eventoRepository.countByOrganizacionId(orgId);

        return new OrganizacionEstadisticasDTO(eventosActivos, boletasVendidas, totalEventos);
    }

    @Transactional(readOnly = true)
    public List<EventosPorCategoriaDTO> obtenerEventosPorCategoriaMiOrganizacion() {
        Organizacion org = organizacionDelUsuario();
        return categoriaRepository.contarEventosPorCategoriaYOrganizacion(org.getId());
    }

    @Transactional(readOnly = true)
    public VentasOrganizacionResumenDTO obtenerResumenVentas() {
        Organizacion org = organizacionDelUsuario();
        Long orgId = org.getId();

        long boletasVendidas = itemCompraRepository.contarBoletasVendidasEventosPublicadosPorOrganizacion(orgId);
        BigDecimal totalIngresos = compraRepository.sumarNetoPorOrganizacion(orgId);
        if (totalIngresos == null) {
            totalIngresos = BigDecimal.ZERO;
        }

        return new VentasOrganizacionResumenDTO(boletasVendidas, totalIngresos);
    }

    @Transactional(readOnly = true)
    public PanelEntradasDTO obtenerPanelEntradas(Pageable pageable) {
        Organizacion org = organizacionDelUsuario();
        Long orgId = org.getId();

        long totalBoletas = itemCompraRepository.contarBoletasVendidasEventosPublicadosPorOrganizacion(orgId);
        long eventosActivos = eventoRepository.countActivosByOrganizacionId(orgId);
        BigDecimal totalIngresos = compraRepository.sumarNetoPorOrganizacion(orgId);
        if (totalIngresos == null) {
            totalIngresos = BigDecimal.ZERO;
        }

        Page<Evento> eventosPage = eventoRepository.findByOrganizacionIdOrderByIdDesc(orgId, pageable);
        List<Evento> eventos = eventosPage.getContent();

        if (eventos.isEmpty()) {
            return PanelEntradasDTO.builder()
                    .totalEventos(eventosPage.getTotalElements())
                    .totalEventosActivos(eventosActivos)
                    .totalBoletasVendidasPorEventosPublicados(totalBoletas)
                    .totalIngresos(totalIngresos)
                    .eventos(new PagedResponse<>(List.of(), eventosPage.getNumber(), eventosPage.getSize(),
                            eventosPage.getTotalElements(), eventosPage.getTotalPages()))
                    .build();
        }

        List<Long> eventoIds = eventos.stream().map(Evento::getId).toList();
        Map<Long, Long> ventasPorEvento = new HashMap<>();
        for (Object[] row : itemCompraRepository.contarVentasPorEventoIds(eventoIds)) {
            ventasPorEvento.put((Long) row[0], ((Number) row[1]).longValue());
        }

        Map<Long, BigDecimal> ingresosPorEvento = new HashMap<>();
        for (Object[] row : compraRepository.sumarNetoPorEventos(eventoIds)) {
            if (row != null && row.length >= 3 && row[0] != null && row[2] != null) {
                ingresosPorEvento.merge((Long) row[0], (BigDecimal) row[2], BigDecimal::add);
            }
        }

        List<EventoEntradasResumenDTO> dtoList = eventos.stream().map(ev -> {
            return EventoEntradasResumenDTO.builder()
                    .id(ev.getId())
                    .nombre(ev.getTitulo())
                    .estado(ev.getEstado())
                    .boletasVendidas(ventasPorEvento.getOrDefault(ev.getId(), 0L))
                    .ingresosGenerados(ingresosPorEvento.getOrDefault(ev.getId(), BigDecimal.ZERO))
                    .build();
        }).toList();

        PagedResponse<EventoEntradasResumenDTO> pagedEventos = new PagedResponse<>(
                dtoList, eventosPage.getNumber(), eventosPage.getSize(),
                eventosPage.getTotalElements(), eventosPage.getTotalPages());

        return PanelEntradasDTO.builder()
                .totalEventos(eventosPage.getTotalElements())
                .totalEventosActivos(eventosActivos)
                .totalBoletasVendidasPorEventosPublicados(totalBoletas)
                .totalIngresos(totalIngresos)
                .eventos(pagedEventos)
                .build();
    }

    @Transactional
    public OrganizacionDTO actualizarPerfilOrganizacion(ActualizarOrganizacionRequest request, MultipartFile imagen) {
        Organizacion organizacion = organizacionDelRepresentante();

        if (request != null) {
            if (request.getRazonSocial() != null && !request.getRazonSocial().isBlank()) {
                organizacion.setRazonSocial(request.getRazonSocial().trim());
            }
            if (request.getDescripcion() != null) {
                organizacion.setDescripcion(request.getDescripcion().trim());
            }
            if (request.getCorreoContacto() != null && !request.getCorreoContacto().isBlank()) {
                organizacion.setCorreoContacto(request.getCorreoContacto().trim());
            }
        }

        if (imagen != null && !imagen.isEmpty()) {
            if (organizacion.getUrlLogo() != null && !organizacion.getUrlLogo().isBlank()) {
                storageService.eliminarImagenDeBucket(storageConfig.getBucketPerfilOrganizacion(), organizacion.getUrlLogo());
            }
            String nuevaUrl = storageService.subirImagenPerfilOrganizacion(imagen);
            organizacion.setUrlLogo(nuevaUrl);
        }

        return toDTO(organizacionRepository.save(organizacion));
    }

    public OrganizacionDTO toDTO(Organizacion o) {
        OrganizacionDTO dto = new OrganizacionDTO();
        dto.setId(o.getId());
        dto.setRepresentante(o.getRepresentante() != null ? o.getRepresentante().getNombreCompleto() : null);
        if (o.getRepresentante() != null) {
            dto.setRepresentanteCorreo(o.getRepresentante().getCorreo());
        }
        dto.setRazonSocial(o.getRazonSocial());
        dto.setNit(o.getNit());
        dto.setCorreoContacto(o.getCorreoContacto());
        dto.setDescripcion(o.getDescripcion());
        dto.setUrlLogo(o.getUrlLogo());
        dto.setFechaCreacion(o.getFechaCreacion());
        dto.setPromedioRating(o.getPromedioRating());
        dto.setTotalValoraciones(o.getTotalValoraciones());
        dto.setTotalSeguidores(o.getTotalSeguidores());
        dto.setTotalEventosCreados(o.getTotalEventosCreados());
        dto.setEventosActivos(eventoRepository.countActivosByOrganizacionId(o.getId()));
        dto.setEventosFinalizados(o.getEventosFinalizados());
        dto.setEventosRechazados(o.getEventosRechazados());
        dto.setNivel(o.getNivel());
        dto.setEstado(o.getEstado());

        if (o.getRepresentante() != null) {
            solicitudRepository.findFirstByRepresentanteId(o.getRepresentante().getId()).ifPresentOrElse(
                    sol -> dto.setEstadoValidacionRut(sol.getEstado().name()),
                    () -> dto.setEstadoValidacionRut((o.getUrlRut() != null && !o.getUrlRut().isBlank()) ? "CARGADO" : "NO_CARGADO")
            );
        } else {
            dto.setEstadoValidacionRut((o.getUrlRut() != null && !o.getUrlRut().isBlank()) ? "CARGADO" : "NO_CARGADO");
        }

        if (o.getUrlRut() != null && !o.getUrlRut().isBlank()) {
            try {
                dto.setUrlRutTemporal(storageService.generarUrlFirmada(o.getUrlRut(), 3600L));
            } catch (Exception e) {
                log.warn("No se pudo generar la URL firmada del RUT para organización {}: {}", o.getId(), e.getMessage());
            }
        }

        return dto;
    }

    private OperadorDTO toOperadorDTO(Usuario u) {
        OperadorDTO dto = new OperadorDTO();
        dto.setId(u.getId());
        dto.setNombreCompleto(u.getNombreCompleto());
        dto.setCorreo(u.getCorreo());
        dto.setPermisosEvento(u.getPermisosEvento() != null ? new HashSet<>(u.getPermisosEvento()) : new HashSet<>());
        return dto;
    }
}

