package com.eventhive.app.service;

import com.eventhive.app.config.SupabaseStorageConfig;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.request.EventoRequest;
import com.eventhive.app.dto.response.EventoBusquedaDTO;
import com.eventhive.app.dto.response.EventoCategoriaDTO;
import com.eventhive.app.dto.response.EventoDTO;
import com.eventhive.app.dto.response.EventoMapaDTO;
import com.eventhive.app.dto.response.EventoOrganizacionDTO;
import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.enums.PermisoEvento;
import com.eventhive.app.enums.TipoNotification;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Categoria;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.CategoriaRepository;
import com.eventhive.app.repository.EventoRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceEvento {

    private final ServiceOrganizacion serviceOrganizacion;
    private final EventoRepository eventoRepository;
    private final CategoriaRepository categoriaRepository;
    private final AuthenticatedUserHelper authHelper;
    private final ServiceNotification serviceNotification;
    private final SupabaseStorageService storageService;
    private final SupabaseStorageConfig storageConfig;
    private final ServiceNivelOrganizacion serviceNivelOrganizacion;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    // Consultas
    @Transactional(readOnly = true)
    public Page<Evento> listarTodos(Pageable pageable) {
        return eventoRepository.findPublicadosVisibles(pageable);
    }

    @Transactional(readOnly = true)
    public Evento obtenerEventoPorId(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id: " + id));
    }

    public Evento obtenerReferenciasEvento(Long id) {
        return eventoRepository.findByIdConReferencias(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Evento> listarPorCategoria(Long categoriaId, Pageable pageable) {
        return eventoRepository.findByCategoriaVisibles(categoriaId, pageable);
    }

    public List<EventoDTO> listarEventosProximos() {
        LocalDate fechaActual = LocalDate.now();
        List<Evento> eventosProximos = eventoRepository.findByFechaAfterAndEstado(fechaActual, EstadoEvento.PUBLICADO);
        return eventosProximos.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public Page<Evento> listarPorOrganizacion(Long organizacionId, Pageable pageable) {
        return eventoRepository.findByOrganizacionIdConReferencias(organizacionId, pageable);
    }

    // Búsqueda pública por título y/o fecha
    @Transactional(readOnly = true)
    public Page<EventoBusquedaDTO> buscarEventos(String titulo, LocalDate fecha, Pageable pageable) {
        String tituloNormalizado = (titulo != null && !titulo.isBlank()) ? titulo.trim() : null;

        return eventoRepository
                .findByTituloOrFechaVisibles(tituloNormalizado, fecha, pageable)
                .map(e -> new EventoBusquedaDTO(
                        e.getId(),
                        e.getTitulo(),
                        e.getCategoria() != null ? e.getCategoria().getNombre() : null));
    }

    // Eventos para el mapa: filtra por categoría y/o radio de distancia
    @Transactional(readOnly = true)
    public List<EventoMapaDTO> buscarParaMapa(Long categoriaId, Double lat, Double lng, Double radioKm) {

        boolean algunoInformado = lat != null || lng != null || radioKm != null;
        boolean todosInformados = lat != null && lng != null && radioKm != null;
        if (algunoInformado && !todosInformados) {
            throw new BusinessException("Para filtrar por distancia debe enviar 'lat', 'lng' y 'radioKm' juntos");
        }

        Double radioMetros = radioKm != null ? radioKm * 1000 : null;

        return eventoRepository.findParaMapa(categoriaId, lat, lng, radioMetros).stream()
                .map(p -> new EventoMapaDTO(
                        p.getId(), p.getTitulo(), p.getDescripcion(),
                        p.getCategoriaNombre(), p.getLatitud(), p.getLongitud()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<Evento> filtrarPorOrganizacionYTitulo(Long organizacionId, String titulo, Pageable pageable) {
        return eventoRepository.findByOrganizacionIdAndTituloConReferencias(organizacionId, titulo, pageable);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MODERADOR') or hasRole('ADMINISTRADOR')")
    public Page<Evento> filtrarCrud(String titulo, Long categoriaId, String estado, Pageable pageable) {
        if (titulo != null && !titulo.isBlank()) {
            return eventoRepository.findByTituloConReferencias(titulo.trim(), pageable);
        }
        if (categoriaId != null) {
            return eventoRepository.findByEstadoConReferencias(EstadoEvento.PUBLICADO, pageable); // fallback seguro
        }
        if (estado != null && !estado.isBlank()) {
            try {
                return eventoRepository.findByEstadoConReferencias(
                        EstadoEvento.valueOf(estado.trim().toUpperCase()), pageable);
            } catch (IllegalArgumentException ex) {
                throw new BusinessException("Estado inválido: " + estado);
            }
        }
        return eventoRepository.findAllConReferencias(pageable);
    }

    // Creación, actualización y eliminación
    @Transactional
    @PreAuthorize("hasAnyRole('REPRESENTANTE','OPERADOR')")
    public Evento crearEvento(EventoRequest request, MultipartFile foto) {
        Usuario usuario = authHelper.usuarioAutenticado();
        Categoria categoria = resolverCategoria(request.getCategoriaId());

        if (usuario.getOrganizacion() == null) {
            throw new BusinessException("Debe pertenecer a una organización para crear eventos");
        }
        validarPermisoSobreEvento(usuario, PermisoEvento.CREAR_EVENTO);

        Organizacion organizacion = usuario.getOrganizacion();
        verificarLimiteDeNivel(organizacion);

        Evento evento = new Evento();
        mapearCampos(evento, request, categoria);
        evento.setOrganizacion(organizacion);
        evento.setCreadoPor(usuario);
        evento.setEstado(PermitePublicacionAutomatica(organizacion));

        if (foto != null && !foto.isEmpty()) {
            evento.setFoto(storageService.subirImagenEvento(foto));
        }

        Evento guardado = eventoRepository.save(evento);

        if (guardado.getEstado() == EstadoEvento.PUBLICADO) {
            serviceNotification.notificarNuevoEvento(guardado);
        }

        serviceOrganizacion.actualizarTotalEventos(organizacion.getId());

        return guardado;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('REPRESENTANTE','OPERADOR')")
    public Evento actualizarEvento(Long id, EventoRequest request, MultipartFile foto) {
        Evento evento = obtenerReferenciasEvento(id);
        verificarPermiso(evento, PermisoEvento.EDITAR_EVENTO);

        if (evento.getEstado() == EstadoEvento.SUSPENDIDO) {
            throw new BusinessException("El evento está suspendido por un administrador y no puede modificarse");
        }

        EstadoEvento estadoAnterior = evento.getEstado();
        Categoria categoria = resolverCategoria(request.getCategoriaId());
        mapearCampos(evento, request, categoria);

        if (estadoAnterior == EstadoEvento.EN_CORRECCION) {
            evento.setEstado(EstadoEvento.PENDIENTE_REVISION);
        }
        if (foto != null && !foto.isEmpty()) {
            eliminarFotoAnterior(evento.getFoto());
            evento.setFoto(storageService.subirImagenEvento(foto));
        }

        Evento guardado = eventoRepository.save(evento);
        notificarCambioSiCorresponde(guardado, estadoAnterior);

        return guardado;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('REPRESENTANTE','OPERADOR')")
    public void eliminarEvento(Long id) {
        Evento evento = obtenerReferenciasEvento(id);
        verificarPermiso(evento, PermisoEvento.CANCELAR_EVENTO);

        if (evento.getEstado() == EstadoEvento.SUSPENDIDO) {
            throw new BusinessException("El evento está suspendido por un administrador y no puede eliminarse");
        }

        Long organizacionId = evento.getOrganizacion().getId();

        eliminarFotoAnterior(evento.getFoto());
        eventoRepository.deleteById(id);

        serviceOrganizacion.actualizarTotalEventos(organizacionId);
    }

    @Transactional
    public void finalizarEventosVencidos(LocalDate hoy) {
        eventoRepository.findByFechaAnteriorYEstado(hoy, EstadoEvento.PUBLICADO).forEach(evento -> {
            evento.setEstado(EstadoEvento.FINALIZADO);
            Organizacion organizacion = evento.getOrganizacion();
            organizacion.setEventosFinalizados(organizacion.getEventosFinalizados() + 1);
            serviceNivelOrganizacion.evaluarAscenso(organizacion);
        });
    }

    //METODOS DE AUXILIARES Y IDOR
    private void verificarLimiteDeNivel(Organizacion organizacion) {
        int maximo = organizacion.getNivel().maxEventosActivos();
        long activos = eventoRepository.countActivosByOrganizacionId(organizacion.getId());
        if (activos >= maximo) {
            throw new BusinessException(
                    "Alcanzaste el límite de " + maximo + " eventos activos para tu nivel " + organizacion.getNivel());
        }
    }

    private EstadoEvento PermitePublicacionAutomatica(Organizacion organizacion) {
        if (organizacion.getNivel().permitePublicacionAutomatica()) {
            return EstadoEvento.PUBLICADO;
        }
        return EstadoEvento.PENDIENTE_REVISION;
    }

    //validar IDOR que el usuario pertenezca a la organizacion del evento
    public void verificarPermiso(Evento evento, PermisoEvento permisoRequerido) {
        Usuario usuario = authHelper.usuarioAutenticado();

        boolean perteneceALaOrganizacion = usuario.getOrganizacion() != null
                && evento.getOrganizacion() != null
                && usuario.getOrganizacion().getId().equals(evento.getOrganizacion().getId());

        if (!perteneceALaOrganizacion) {
            throw new BusinessException("No autorizado para modificar este evento");
        }
        validarPermisoSobreEvento(usuario, permisoRequerido);
    }

    private void validarPermisoSobreEvento(Usuario usuario, PermisoEvento permisoRequerido) {
        Organizacion organizacion = usuario.getOrganizacion();
        if (organizacion == null)
            throw new BusinessException("No perteneces a ninguna organización");

        boolean esRepresentante = organizacion.getRepresentante().getId().equals(usuario.getId());
        if (esRepresentante) return; // el representante tiene control total

        if (!usuario.getPermisosEvento().contains(permisoRequerido))
            throw new BusinessException("No tienes permiso para realizar esta acción");
    }

    private Categoria resolverCategoria(Long categoriaId) {
        if (categoriaId == null) {
            throw new BusinessException("El categoriaId es requerido");
        }
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + categoriaId));
    }

    private void eliminarFotoAnterior(String urlFoto) {
        if (urlFoto == null || urlFoto.isBlank()) {
            return;
        }
        String nombre = storageService.extraerNombreArchivo(urlFoto);
        storageService.eliminarArchivo(storageConfig.getBucketEventos(), nombre);
    }

    private void notificarCambioSiCorresponde(Evento guardado, EstadoEvento estadoAnterior) {
        EstadoEvento estadoActual = guardado.getEstado();

        if (estadoActual == EstadoEvento.CANCELADO) {
            serviceNotification.notificarCambioEvento(guardado, TipoNotification.EVENTO_CANCELADO);
            return;
        }

        boolean estabaPublicado = estadoAnterior == EstadoEvento.PUBLICADO;
        boolean siguePublicado = estadoActual == EstadoEvento.PUBLICADO;

        if (estabaPublicado && siguePublicado) {
            serviceNotification.notificarCambioEvento(guardado, TipoNotification.EVENTO_MODIFICADO);
        }
    }

    //METODOS DE MAPEO
    public EventoDTO toDTO(Evento e) {
        EventoDTO dto = new EventoDTO();
        dto.setId(e.getId());
        dto.setTitulo(e.getTitulo());
        dto.setDescripcion(e.getDescripcion());
        dto.setLugar(e.getLugar());
        dto.setFoto(e.getFoto() != null && !e.getFoto().isBlank() ? e.getFoto() : null);
        dto.setFecha(e.getFecha());
        dto.setHora(e.getHora());
        dto.setLocalidades(e.getLocalidades());
        dto.setEstado(e.getEstado());

        if (e.getUbicacion() != null) {
            dto.setLatitud(e.getUbicacion().getY());
            dto.setLongitud(e.getUbicacion().getX());
        }

        if (e.getCategoria() != null) {
            dto.setCategoria(new EventoCategoriaDTO(
                    e.getCategoria().getId(),
                    e.getCategoria().getNombre()));
        }

        if (e.getOrganizacion() != null) {
            dto.setOrganizacion(new EventoOrganizacionDTO(
                    e.getOrganizacion().getId(),
                    e.getOrganizacion().getRazonSocial()));
        }

        return dto;
    }

    public PagedResponse<EventoDTO> toPagedDTO(Page<Evento> page) {
        return new PagedResponse<>(
                page.getContent().stream().map(this::toDTO).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private void mapearCampos(Evento evento, EventoRequest req, Categoria categoria) {
        evento.setTitulo(req.getTitulo());
        evento.setDescripcion(req.getDescripcion());
        evento.setFecha(req.getFecha());
        evento.setHora(req.getHora());
        evento.setLugar(req.getLugar());
        evento.setCategoria(categoria);

        // JTS usa orden (x=longitud, y=latitud)
        Point punto = GEOMETRY_FACTORY.createPoint(new Coordinate(req.getLongitud(), req.getLatitud()));
        evento.setUbicacion(punto);
    }
}