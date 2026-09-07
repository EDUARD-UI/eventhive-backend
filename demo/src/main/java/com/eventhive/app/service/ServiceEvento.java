package com.eventhive.app.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.eventhive.app.config.SupabaseStorageConfig;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.request.EventoRequest;
import com.eventhive.app.dto.response.EventoBusquedaDTO;
import com.eventhive.app.dto.response.EventoCategoriaDTO;
import com.eventhive.app.dto.response.EventoDTO;
import com.eventhive.app.dto.response.EventoMapaDTO;
import com.eventhive.app.dto.response.EventoOrganizacionDTO;
import com.eventhive.app.dto.response.LocalidadDTO;
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
import com.eventhive.app.repository.ModeracionEventoRepository;
import com.eventhive.app.repository.OrganizacionRepository;
import com.eventhive.app.repository.TiqueteRepository;
import com.eventhive.app.repository.specification.EventoSpecification;
import com.eventhive.app.utils.AuthenticatedUserHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceEvento {

    private final ServiceOrganizacion serviceOrganizacion;
    private final ServiceNivelOrganizacion serviceNivelOrganizacion;
    private final ServiceNotification serviceNotification;
    private final EventoRepository eventoRepository;
    private final CategoriaRepository categoriaRepository;
    private final TiqueteRepository tiqueteRepository;
    private final ModeracionEventoRepository moderacionEventoRepository;
    private final OrganizacionRepository organizacionRepository;
    private final AuthenticatedUserHelper authHelper;
    private final SupabaseStorageService storageService;
    private final SupabaseStorageConfig storageConfig;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    // CONSULTAS
    public Page<Evento> listarTodos(Pageable pageable) {
        return eventoRepository.findPublicadosVisibles(pageable);
    }

    public Evento obtenerEventoAdministrativo(Long id) {
        return eventoRepository.findByIdConReferencias(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));
    }

    // solo devuelve evento en estado.PUBLICADO
    public Evento obtenerEventoPublicoPorId(Long id) {
        return eventoRepository.findByIdAndEstadoConReferencias(id, EstadoEvento.PUBLICADO)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id: " + id));
    }

    //el evento debe pertenecer a la organización del usuario autenticado
    public Evento obtenerEventoDeOrganizacionPorId(Long organizacionId, Long id) {
        Evento evento = obtenerReferenciasEvento(id);
        boolean perteneceALaOrganizacion = evento.getOrganizacion() != null
                && evento.getOrganizacion().getId().equals(organizacionId);
        if (!perteneceALaOrganizacion) {
            throw new ResourceNotFoundException("Evento no encontrado con id: " + id);
        }
        return evento;
    }

    public Evento obtenerReferenciasEvento(Long id) {
        return eventoRepository.findByIdConReferencias(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Evento> listarPorCategoria(Long categoriaId, Pageable pageable) {
        return eventoRepository.findByCategoriaVisibles(categoriaId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Evento> listarEventosProximos(Pageable pageable) {
        return eventoRepository.findProximosPublicados(
                LocalDate.now(),
                LocalTime.now(),
                pageable);
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
                .findByTituloAndFechaVisibles(tituloNormalizado, fecha, pageable)
                .map(this::toEventoBusquedaDTO);
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
    public Page<Evento> filtrarCrud(String titulo, Long categoriaId, String estado, Pageable pageable) {
        Specification<Evento> filtro = EventoSpecification.build(titulo, categoriaId, estado);
        return eventoRepository.findAll(filtro, pageable);
    }

    // OPERACIONES CRUD
    @Transactional
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
        mapearCamposRequest(evento, request, categoria);
        evento.setOrganizacion(organizacion);
        evento.setCreadoPor(usuario);
        evento.setEstado(permitePublicacionAutomatica(organizacion));

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
    public Evento actualizarEvento(Long id, EventoRequest request, MultipartFile foto) {
        Evento evento = obtenerReferenciasEvento(id);
        verificarPermiso(evento, PermisoEvento.EDITAR_EVENTO);

        if (evento.getEstado() == EstadoEvento.SUSPENDIDO) {
            throw new BusinessException("El evento está suspendido por un administrador y no puede modificarse");
        }

        EstadoEvento estadoAnterior = evento.getEstado();
        Categoria categoria = resolverCategoria(request.getCategoriaId());
        mapearCamposRequest(evento, request, categoria);

        if (estadoAnterior == EstadoEvento.EN_CORRECCION) {
            transicionarEstado(evento, EstadoEvento.PENDIENTE_REVISION);
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
    public void cancelarEvento(Long id) {
        Evento evento = obtenerEventoAdministrativo(id);
        verificarPermiso(evento, PermisoEvento.CANCELAR_EVENTO);

        if (evento.getEstado() != EstadoEvento.PUBLICADO) {
            throw new BusinessException("Solo se pueden cancelar eventos PUBLICADOS");
        }

        transicionarEstado(evento, EstadoEvento.CANCELADO);
        serviceNotification.notificarCambioEvento(evento, TipoNotification.EVENTO_CANCELADO);
    }

    // Retira un evento en PENDIENTE_REVISION y lo devuelve a BORRADOR, sin eliminarlo.
    @Transactional
    public void retirarEvento(Long id) {
        Evento evento = obtenerEventoAdministrativo(id);
        verificarPermiso(evento, PermisoEvento.EDITAR_EVENTO);

        if (evento.getEstado() != EstadoEvento.PENDIENTE_REVISION) {
            throw new BusinessException(
                    "Solo se pueden retirar eventos en estado PENDIENTE_REVISION");
        }

        transicionarEstado(evento, EstadoEvento.BORRADOR);
    }

    @Transactional
    public void eliminarEvento(Long id) {
        Evento evento = obtenerEventoAdministrativo(id);
        verificarPermiso(evento, PermisoEvento.CANCELAR_EVENTO);

        if (evento.getEstado() != EstadoEvento.BORRADOR) {
            throw new BusinessException("Solo se pueden eliminar físicamente eventos en BORRADOR");
        }

        if (moderacionEventoRepository.existsByEventoId(id) || tiqueteRepository.existsByEventoId(id)) {
            throw new BusinessException("El evento tiene historial o ventas y debe conservarse");
        }

        eliminarFotoAnterior(evento.getFoto());
        eventoRepository.delete(evento);
    }

    @Transactional
    public void finalizarEventosVencidos(LocalDate hoy, LocalTime horaActual) {
        List<Evento> vencidos = eventoRepository.findVencidosYEstado(hoy, horaActual, EstadoEvento.PUBLICADO);

        for (Evento evento : vencidos) {
            transicionarEstado(evento, EstadoEvento.FINALIZADO);
            actualizarMetricasPorFinalizacion(evento.getOrganizacion());
        }
    }

    private void actualizarMetricasPorFinalizacion(Organizacion organizacion) {
        organizacion.setEventosFinalizados(organizacion.getEventosFinalizados() + 1);
        organizacionRepository.save(organizacion);
        serviceNivelOrganizacion.evaluarAscenso(organizacion);
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

    private EstadoEvento permitePublicacionAutomatica(Organizacion organizacion) {
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
        dto.setEstado(e.getEstado());
        dto.setLocalidades(
                e.getLocalidades() == null
                        ? List.of()
                        : e.getLocalidades().stream().map(localidad -> {
                    LocalidadDTO l = new LocalidadDTO();
                    l.setId(localidad.getId());
                    l.setNombre(localidad.getNombre());
                    l.setPrecio(localidad.getPrecio());
                    l.setCapacidad(localidad.getCapacidad());
                    l.setDisponibles(localidad.getDisponibles());
                    return l;
                }).toList()
        );

        if (e.getUbicacion() != null) {
            dto.setLatitud(e.getUbicacion().getY());
            dto.setLongitud(e.getUbicacion().getX());
        }

        if (e.getCategoria() != null) {
            dto.setCategoria(toEventoCategoriaDTO(e));
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

    private EventoBusquedaDTO toEventoBusquedaDTO(Evento evento){
        EventoBusquedaDTO dto = new EventoBusquedaDTO();
        dto.setId(evento.getId());
        dto.setNombreCategoria(evento.getCategoria().getNombre());
        dto.setTitulo(evento.getTitulo());
        return dto;
    }

    private EventoCategoriaDTO toEventoCategoriaDTO(Evento evento){
        EventoCategoriaDTO dto = new EventoCategoriaDTO();
        dto.setId(evento.getCategoria().getId());
        dto.setNombre(evento.getCategoria().getNombre());
        return dto;
    }

    private void mapearCamposRequest(Evento evento, EventoRequest req, Categoria categoria) {
        validarFechaHoraEvento(req.getFecha(), req.getHora());

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

    private void validarFechaHoraEvento(LocalDate fecha, LocalTime hora) {
        LocalDateTime fechaHora = LocalDateTime.of(fecha, hora);
        if (fechaHora.isBefore(LocalDateTime.now())) {
            throw new BusinessException("La fecha y hora del evento no pueden estar en el pasado");
        }
    }

    //VALIDACIONES EN CAMBIO DE ESTADOS
    private static final Map<EstadoEvento, Set<EstadoEvento>> TRANSICIONES = Map.of(
            EstadoEvento.BORRADOR,
            Set.of(EstadoEvento.PENDIENTE_REVISION),

            EstadoEvento.PENDIENTE_REVISION,
            Set.of(
                    EstadoEvento.PUBLICADO,
                    EstadoEvento.EN_CORRECCION,
                    EstadoEvento.RECHAZADO,
                    EstadoEvento.BORRADOR
            ),

            EstadoEvento.EN_CORRECCION,
            Set.of(EstadoEvento.PENDIENTE_REVISION, EstadoEvento.BORRADOR),

            EstadoEvento.PUBLICADO,
            Set.of(EstadoEvento.CANCELADO, EstadoEvento.FINALIZADO, EstadoEvento.SUSPENDIDO),

            EstadoEvento.SUSPENDIDO,
            Set.of(EstadoEvento.PUBLICADO),

            EstadoEvento.CANCELADO,
            Set.of(),

            EstadoEvento.FINALIZADO,
            Set.of(),

            EstadoEvento.RECHAZADO,
            Set.of()
    );

    public void transicionarEstado(Evento evento, EstadoEvento nuevoEstado) {
        EstadoEvento actual = evento.getEstado();

        if (!TRANSICIONES.getOrDefault(actual, Set.of()).contains(nuevoEstado)) {
            throw new BusinessException(
                    "Transición no permitida: " + actual + " -> " + nuevoEstado);
        }

        evento.setEstado(nuevoEstado);
    }
}