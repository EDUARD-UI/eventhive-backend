package com.eventhive.app.service;

import java.math.BigDecimal;
import java.util.HashMap;
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
    private final SupabaseStorageService storageService;
    private final SupabaseStorageConfig storageConfig;
    private final ServiceNotification serviceNotification;
    private final ItemCompraRepository itemCompraRepository;
    private final LocalidadRepository localidadRepository;
    private final CategoriaRepository categoriaRepository;

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
        if (usuario.getOrganizacion() == null)
            throw new BusinessException("Aún no tiene un perfil de organización aprobado");
        Organizacion organizacion = organizacionRepository.findByIdConRepresentante(usuario.getOrganizacion().getId())
                .orElse(usuario.getOrganizacion());
        return toDTO(organizacion);
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
    public void actualizarPermisos(Long operadorId, PermisosOperadorRequest request) {
        Organizacion organizacion = organizacionDelRepresentante();
        Usuario operador = obtenerOperadorDeLaOrganizacion(operadorId, organizacion);

        operador.setPermisosEvento(Set.copyOf(request.getPermisos()));
        usuarioRepository.save(operador);
    }

    @Transactional
    public void expulsarOperador(Long operadorId) {
        Organizacion organizacion = organizacionDelRepresentante();
        Usuario operador = obtenerOperadorDeLaOrganizacion(operadorId, organizacion);

        operador.setOrganizacion(null);
        operador.setPermisosEvento(Set.of());
        operador.setRol(rolesRepository.findByNombre("CLIENTE")
                .orElseThrow(() -> new ResourceNotFoundException("Rol CLIENTE no configurado")));
        usuarioRepository.save(operador);

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
        if (usuario.getOrganizacion() == null) {
            throw new BusinessException("El usuario no pertenece a ninguna organización");
        }
        return usuario.getOrganizacion();
    }

    @Transactional(readOnly = true)
    public OrganizacionEstadisticasDTO obtenerEstadisticasMiOrganizacion() {
        Organizacion org = organizacionDelUsuario();
        Long orgId = org.getId();

        long eventosActivos = eventoRepository.countActivosByOrganizacionId(orgId);
        long boletasVendidas = itemCompraRepository.contarBoletasVendidasPorOrganizacion(orgId);
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

        long boletasVendidas = itemCompraRepository.contarBoletasVendidasPorOrganizacion(orgId);
        BigDecimal totalIngresos = itemCompraRepository.sumarIngresosPorOrganizacion(orgId);
        if (totalIngresos == null) {
            totalIngresos = BigDecimal.ZERO;
        }

        return new VentasOrganizacionResumenDTO(boletasVendidas, totalIngresos);
    }

    @Transactional(readOnly = true)
    public PanelEntradasDTO obtenerPanelEntradas(Pageable pageable) {
        Organizacion org = organizacionDelUsuario();
        Long orgId = org.getId();

        long totalBoletas = itemCompraRepository.contarBoletasVendidasPorOrganizacion(orgId);
        BigDecimal totalIngresos = itemCompraRepository.sumarIngresosPorOrganizacion(orgId);
        if (totalIngresos == null) {
            totalIngresos = BigDecimal.ZERO;
        }

        Page<Evento> eventosPage = eventoRepository.findByOrganizacionIdOrderByIdDesc(orgId, pageable);
        List<Evento> eventos = eventosPage.getContent();

        if (eventos.isEmpty()) {
            return PanelEntradasDTO.builder()
                    .totalBoletasVendidas(totalBoletas)
                    .totalIngresos(totalIngresos)
                    .eventos(new PagedResponse<>(List.of(), eventosPage.getNumber(), eventosPage.getSize(),
                            eventosPage.getTotalElements(), eventosPage.getTotalPages()))
                    .build();
        }

        List<Long> eventoIds = eventos.stream().map(Evento::getId).toList();
        List<Localidad> todasLocalidades = localidadRepository.findByEventoIdIn(eventoIds);
        Map<Long, List<Localidad>> localidadesPorEvento = todasLocalidades.stream()
                .collect(Collectors.groupingBy(loc -> loc.getEvento().getId()));

        List<Long> localidadIds = todasLocalidades.stream().map(Localidad::getId).toList();
        Map<Long, Long> ventasMap = new HashMap<>();
        if (!localidadIds.isEmpty()) {
            List<Object[]> ventasRows = itemCompraRepository.contarVentasPorLocalidadIds(localidadIds);
            for (Object[] row : ventasRows) {
                Long locId = (Long) row[0];
                Long cantidad = ((Number) row[1]).longValue();
                ventasMap.put(locId, cantidad);
            }
        }

        List<EventoEntradasResumenDTO> dtoList = eventos.stream().map(ev -> {
            List<Localidad> locs = localidadesPorEvento.getOrDefault(ev.getId(), List.of());
            List<LocalidadEntradasDTO> locDTOs = locs.stream().map(loc -> {
                long vendidas = ventasMap.getOrDefault(loc.getId(), 0L);
                if (vendidas == 0 && loc.getCapacidad() > loc.getDisponibles()) {
                    vendidas = loc.getCapacidad() - loc.getDisponibles();
                }
                double porcentaje = loc.getCapacidad() > 0
                        ? Math.min(100.0, Math.round(((double) vendidas / loc.getCapacidad()) * 1000.0) / 10.0)
                        : 0.0;
                return LocalidadEntradasDTO.builder()
                        .id(loc.getId())
                        .nombre(loc.getNombre())
                        .precio(loc.getPrecio())
                        .capacidad(loc.getCapacidad())
                        .disponibles(loc.getDisponibles())
                        .boletasVendidas(vendidas)
                        .porcentajeVendido(porcentaje)
                        .build();
            }).toList();

            return EventoEntradasResumenDTO.builder()
                    .id(ev.getId())
                    .nombre(ev.getTitulo())
                    .localidades(locDTOs)
                    .build();
        }).toList();

        PagedResponse<EventoEntradasResumenDTO> pagedEventos = new PagedResponse<>(
                dtoList, eventosPage.getNumber(), eventosPage.getSize(),
                eventosPage.getTotalElements(), eventosPage.getTotalPages());

        return PanelEntradasDTO.builder()
                .totalBoletasVendidas(totalBoletas)
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
        dto.setEventosFinalizados(o.getEventosFinalizados());
        dto.setEventosRechazados(o.getEventosRechazados());
        dto.setNivel(o.getNivel());
        dto.setEstado(o.getEstado());
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

