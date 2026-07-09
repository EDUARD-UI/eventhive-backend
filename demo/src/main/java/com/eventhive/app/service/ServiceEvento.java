package com.eventhive.app.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.eventhive.app.config.SupabaseStorageConfig;
import com.eventhive.app.dto.EventoBusquedaDTO;
import com.eventhive.app.dto.EventoCategoriaDTO;
import com.eventhive.app.dto.EventoDTO;
import com.eventhive.app.dto.EventoOrganizadorDTO;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.request.EventoRequest;
import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.enums.TipoNotification;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Categoria;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.CategoriaRepository;
import com.eventhive.app.repository.EventoRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceEvento {

    private final ServiceMetricasOrganizacion serviceMetricasOrganizacion;
    private final EventoRepository           eventoRepository;
    private final CategoriaRepository        categoriaRepository;
    private final AuthenticatedUserHelper    authHelper;
    private final ServiceNotification        serviceNotification;
    private final SupabaseStorageService     storageService;
    private final SupabaseStorageConfig      storageConfig;
    private final ServiceNivelOrganizacion   serviceNivelOrganizacion;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Consultas
    @Transactional(readOnly = true)
    public Page<Evento> listarTodos(Pageable pageable) {
        return eventoRepository.findPublicadosVisibles(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Evento> listarPorCategoria(Long categoriaId, Pageable pageable) {
        return eventoRepository.findByCategoriaVisibles(categoriaId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Evento> listarPorOrganizador(Long organizadorId, Pageable pageable) {
        return eventoRepository.findByOrganizadorIdConReferencias(organizadorId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Evento> buscarPorOrganizadorYTitulo(Long organizadorId, String titulo, Pageable pageable) {
        return eventoRepository.findByOrganizadorIdAndTituloConReferencias(organizadorId, titulo, pageable);
    }

    @Transactional(readOnly = true)
    public Page<EventoBusquedaDTO> buscarPorTitulo(String titulo, Pageable pageable) {
        return eventoRepository
                .findByTituloVisibles(titulo, pageable)
                .map(e -> new EventoBusquedaDTO(
                        e.getId(),
                        e.getTitulo(),
                        e.getCategoria() != null ? e.getCategoria().getNombre() : null));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MODERADOR') or hasRole('ADMINISTRADOR')")
    public Page<Evento> buscarAdmin(String titulo, Long categoriaId, String estado, Pageable pageable) {
        if (titulo != null && !titulo.isBlank())
            return eventoRepository.findByTituloConReferencias(titulo.trim(), pageable);
        if (categoriaId != null)
            return eventoRepository.findByEstadoConReferencias(EstadoEvento.PUBLICADO, pageable); // fallback seguro
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

    @Transactional(readOnly = true)
    public Evento obtenerPorId(Long id) {
        return eventoRepository.findByIdConReferencias(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id: " + id));
    }

    @Transactional(readOnly = true)
    public Evento obtenerReferencia(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id: " + id));
    }

    // Creación, actualización y eliminación
    @Transactional
    @PreAuthorize("hasRole('ORGANIZACION')")
    public Evento crearEvento(EventoRequest request, MultipartFile foto) {
        Usuario organizador = authHelper.usuarioAutenticado();
        Categoria categoria = resolverCategoria(request.getCategoriaId());

        // Solo un organizador con SolicitudVerificacion aprobada tiene perfil de Organizacion
        if (organizador.getOrganizacion() == null)
            throw new BusinessException("Debe completar la verificación como organización antes de crear eventos");

        verificarLimiteDeNivel(organizador);

        Evento evento = new Evento();
        mapearCampos(evento, request, categoria);
        evento.setOrganizador(organizador);
        evento.setEstado(estadoInicial(organizador));

        if (foto != null && !foto.isEmpty())
            evento.setFoto(storageService.subirImagenEvento(foto));

        Evento guardado = eventoRepository.save(evento);

        if (guardado.getEstado() == EstadoEvento.PUBLICADO)
            serviceNotification.notificarNuevoEvento(guardado);

        serviceMetricasOrganizacion.actualizarTotalEventos(organizador.getId());

        return guardado;
    }

    @Transactional
    @PreAuthorize("hasRole('ORGANIZACION')")
    public Evento actualizarEvento(Long id, EventoRequest request, MultipartFile foto) {
        Evento evento = obtenerPorId(id);
        verificarPermiso(evento);

        if (evento.getEstado() == EstadoEvento.SUSPENDIDO)
            throw new BusinessException("El evento está suspendido por un administrador y no puede modificarse");

        EstadoEvento estadoAnterior = evento.getEstado();
        Categoria categoria = resolverCategoria(request.getCategoriaId());
        mapearCampos(evento, request, categoria);

        if (estadoAnterior == EstadoEvento.EN_CORRECCION)
            evento.setEstado(EstadoEvento.PENDIENTE_REVISION); // la corrección vuelve a entrar a la cola

        if (foto != null && !foto.isEmpty()) {
            eliminarFotoAnterior(evento.getFoto());
            evento.setFoto(storageService.subirImagenEvento(foto));
        }

        Evento guardado = eventoRepository.save(evento);
        notificarCambioSiCorresponde(guardado, estadoAnterior);

        return guardado;
    }

    @Transactional
    @PreAuthorize("hasRole('ORGANIZACION')")
    public void eliminarEvento(Long id) {
        Evento evento = obtenerPorId(id);
        verificarPermiso(evento);

        if (evento.getEstado() == EstadoEvento.SUSPENDIDO)
            throw new BusinessException("El evento está suspendido por un administrador y no puede eliminarse");

        Long organizadorId = evento.getOrganizador().getId();

        eliminarFotoAnterior(evento.getFoto());
        eventoRepository.deleteById(id);

        serviceMetricasOrganizacion.actualizarTotalEventos(organizadorId);
    }

    // Marca como FINALIZADO cualquier evento publicado cuya fecha ya pasó, y evalúa el ascenso del organizador
    @Transactional
    public void finalizarEventosVencidos(LocalDate hoy) {
        eventoRepository.findByFechaAnteriorYEstado(hoy, EstadoEvento.PUBLICADO).forEach(evento -> {
            evento.setEstado(EstadoEvento.FINALIZADO);
            var organizacion = evento.getOrganizador().getOrganizacion();
            organizacion.setEventosFinalizados(organizacion.getEventosFinalizados() + 1);
            serviceNivelOrganizacion.evaluarAscenso(organizacion);
        });
    }

    // Nivel de confianza de la organización

    // Bloquea la creación si la organización ya alcanzó el máximo de eventos activos de su nivel
    private void verificarLimiteDeNivel(Usuario organizador) {
        int maximo = organizador.getOrganizacion().getNivel().maxEventosActivos();
        long activos = eventoRepository.countActivosByOrganizadorId(organizador.getId());
        if (activos >= maximo)
            throw new BusinessException(
                    "Alcanzaste el límite de " + maximo + " eventos activos para tu nivel " + organizador.getOrganizacion().getNivel());
    }

    //una organización de nivel máximo publica directo; el resto entra a PENDIENTE_REVISION
    private EstadoEvento estadoInicial(Usuario organizador) {
        if (organizador.getOrganizacion().getNivel().permitePublicacionAutomatica())
            return EstadoEvento.PUBLICADO;
        return EstadoEvento.PENDIENTE_REVISION;
    }

    // Permisos: crear/editar/eliminar un evento (y su contenido, ej. localidades) es
    // exclusivo del organizador dueño. El ADMINISTRADOR no gestiona el contenido del
    // evento; su única facultad sobre eventos ya publicados es suspenderlo/reactivarlo
    // (ver ServiceModeracion.suspenderEvento/reactivarEvento).
    public void verificarPermiso(Evento evento) {
        Usuario u = authHelper.usuarioAutenticado();
        boolean esOrganizador = evento.getOrganizador() != null
                && u.getId().equals(evento.getOrganizador().getId());
        if (!esOrganizador)
            throw new BusinessException("No autorizado para modificar este evento");
    }

    // Mapeo DTO
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

        if (e.getCategoria() != null)
            dto.setCategoria(new EventoCategoriaDTO(
                    e.getCategoria().getId(),
                    e.getCategoria().getNombre()));

        if (e.getOrganizador() != null)
            dto.setOrganizador(new EventoOrganizadorDTO(
                    e.getOrganizador().getId(),
                    e.getOrganizador().getNombreCompleto()));

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

    // Métodos auxiliares

    private void mapearCampos(Evento evento, EventoRequest req, Categoria categoria) {
        evento.setTitulo(req.getTitulo());
        evento.setDescripcion(req.getDescripcion());
        evento.setFecha(req.getFecha());
        evento.setHora(req.getHora());
        evento.setLugar(req.getLugar());
        evento.setLatitud(req.getLatitud());
        evento.setLongitud(req.getLongitud());
        evento.setCategoria(categoria);

        if (req.getFechaPublicacion() != null && !req.getFechaPublicacion().isBlank())
            evento.setFechaPublicacion(LocalDateTime.parse(req.getFechaPublicacion(), FMT));
        else if (evento.getFechaPublicacion() == null)
            evento.setFechaPublicacion(LocalDateTime.now());
    }

    private Categoria resolverCategoria(Long categoriaId) {
        if (categoriaId == null)
            throw new BusinessException("El categoriaId es requerido");
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + categoriaId));
    }

    private void eliminarFotoAnterior(String urlFoto) {
        if (urlFoto == null || urlFoto.isBlank()) return;
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
        boolean siguePublicado  = estadoActual   == EstadoEvento.PUBLICADO;

        if (estabaPublicado && siguePublicado)
            serviceNotification.notificarCambioEvento(guardado, TipoNotification.EVENTO_MODIFICADO);
    }
}
