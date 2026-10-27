package com.eventhive.app.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.ZoneId;
import org.springframework.data.domain.PageRequest;
import com.eventhive.app.dto.response.PrimerEventoDestacadoDTO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.eventhive.app.config.CacheConfig;
import com.eventhive.app.config.SupabaseStorageConfig;
import com.eventhive.app.dto.PagedResponse;
import com.eventhive.app.dto.request.EventoRequest;
import com.eventhive.app.dto.response.EventoCategoriaDTO;
import com.eventhive.app.dto.response.EventoDTO;
import com.eventhive.app.dto.response.EventoDestacadoDTO;
import com.eventhive.app.dto.response.EventoMapaDTO;
import com.eventhive.app.dto.response.EventoOrganizacionDTO;
import com.eventhive.app.dto.response.EventosResumenOrganizacionDTO;
import com.eventhive.app.dto.response.LocalidadDTO;
import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.enums.EstadoOrganizacion;
import com.eventhive.app.enums.MotivosRechazos;
import com.eventhive.app.enums.PermisoEvento;
import com.eventhive.app.enums.TipoNotification;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import com.eventhive.app.model.Categoria;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Localidad;
import com.eventhive.app.model.ModeracionEvento;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.auto.ResultadoReglaModeracion;
import com.eventhive.app.auto.ServiceModeracionAutomatica;
import com.eventhive.app.auto.VeredictoModeracion;
import com.eventhive.app.repository.CategoriaRepository;
import com.eventhive.app.repository.EventoRepository;
import com.eventhive.app.repository.LocalidadRepository;
import com.eventhive.app.repository.ModeracionEventoRepository;
import com.eventhive.app.repository.OrganizacionRepository;
import com.eventhive.app.repository.TiqueteRepository;
import com.eventhive.app.repository.specification.EventoSpecification;
import com.eventhive.app.utils.AuthenticatedUserHelper;
import com.eventhive.app.utils.OrdenEventos;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceEvento {

    private final ServiceOrganizacion serviceOrganizacion;
    private final ServiceNivelOrganizacion serviceNivelOrganizacion;
    private final ServiceNotification serviceNotification;
    private final ServiceModeracionAutomatica serviceModeracionAutomatica;
    private final EventoRepository eventoRepository;
    private final CategoriaRepository categoriaRepository;
    private final LocalidadRepository localidadRepository;
    private final TiqueteRepository tiqueteRepository;
    private final ModeracionEventoRepository moderacionEventoRepository;
    private final OrganizacionRepository organizacionRepository;
    private final AuthenticatedUserHelper authHelper;
    private final SupabaseStorageService storageService;
    private final SupabaseStorageConfig storageConfig;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    // CONSULTAS
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_EVENTOS_PUBLICOS, key = "{'todos', #pageable.pageNumber, #pageable.pageSize}")
    public Page<Evento> listarTodos(Pageable pageable) {
        return eventoRepository.findPublicadosVisibles(OrdenEventos.estable(pageable));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_EVENTOS_PUBLICOS, key = "{#categoriaId, #fecha, #pageable.pageNumber, #pageable.pageSize}")
    public Page<Evento> listarPublicos(Long categoriaId, LocalDate fecha, Pageable pageable) {
        Specification<Evento> spec = (root, query, cb) -> {
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("categoria", JoinType.INNER);
                root.fetch("organizacion", JoinType.INNER);
            }
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("estado"), EstadoEvento.PUBLICADO));
            if (categoriaId != null) {
                predicates.add(cb.equal(root.get("categoria").get("id"), categoriaId));
            }
            if (fecha != null) {
                predicates.add(cb.equal(root.get("fecha"), fecha));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return eventoRepository.findAll(spec, OrdenEventos.promocionadosPrimero(pageable));
    }

    // Solo eventos promocionados que además están PUBLICADOS.
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_EVENTOS_DESTACADOS, key = "{#pageable.pageNumber, #pageable.pageSize}")
    public Page<Evento> listarDestacados(Pageable pageable) {
        return eventoRepository.findDestacadosPublicados(pageable);
    }

    // Devuelve el primer evento destacado (promocionado: true y PUBLICADO)
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_PRIMER_DESTACADO, key = "'primerDestacado'")
    public PrimerEventoDestacadoDTO obtenerPrimerEventoDestacado() {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Bogota"));
        LocalTime hora = LocalTime.now(ZoneId.of("America/Bogota"));

        List<Evento> proximos = eventoRepository.findPrimerDestacadoProximo(hoy, hora, PageRequest.of(0, 1));
        Evento evento = !proximos.isEmpty()
                ? proximos.get(0)
                : eventoRepository.findPrimerDestacado(PageRequest.of(0, 1)).stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("No se encontró ningún evento destacado activo"));

        String urlImagen = (evento.getUrlImagenDestacado() != null && !evento.getUrlImagenDestacado().isBlank())
                ? evento.getUrlImagenDestacado()
                : evento.getFoto();

        return PrimerEventoDestacadoDTO.builder()
                .id(evento.getId())
                .nombre(evento.getTitulo())
                .urlImagen(urlImagen)
                .fechaCreacion(evento.getFechaCreacion())
                .fecha(evento.getFecha())
                .hora(evento.getHora())
                .build();
    }

    // Consulta pública de eventos de una organización (solo PUBLICADO) con carga de referencias (evita N+1)
    @Transactional(readOnly = true)
    public Page<Evento> listarPublicosPorOrganizacion(Long organizacionId, Long categoriaId, Pageable pageable) {
        if (!organizacionRepository.existsById(organizacionId)) {
            throw new ResourceNotFoundException("Organización no encontrada con id: " + organizacionId);
        }

        if (categoriaId == null) {
            return eventoRepository.findPublicadosByOrganizacionId(organizacionId, pageable);
        }

        Specification<Evento> spec = (root, query, cb) -> {
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("categoria", JoinType.INNER);
                root.fetch("organizacion", JoinType.INNER);
            }
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizacion").get("id"), organizacionId));
            predicates.add(cb.equal(root.get("estado"), EstadoEvento.PUBLICADO));
            predicates.add(cb.equal(root.get("categoria").get("id"), categoriaId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return eventoRepository.findAll(spec, OrdenEventos.promocionadosPrimero(pageable));
    }

    @Transactional(readOnly = true)
    public Evento obtenerEventoAdministrativo(Long id) {
        return eventoRepository.findByIdConReferencias(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));
    }

    // solo devuelve evento en estado.PUBLICADO
    @Transactional(readOnly = true)
    public Evento obtenerEventoPublicoPorId(Long id) {
        return eventoRepository.findByIdAndEstadoConReferencias(id, EstadoEvento.PUBLICADO)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id: " + id));
    }

    // devuelve evento con cualquier estado (solo administración usa este método)
    @Transactional(readOnly = true)
    public Evento obtenerEventoAdminPorId(Long id) {
        return obtenerEventoPorId(id);
    }

    @Transactional(readOnly = true)
    public Evento obtenerEventoPorId(Long id) {
        return obtenerReferenciasEvento(id);
    }

    //el evento debe pertenecer a la organización del usuario autenticado
    @Transactional(readOnly = true)
    public Evento obtenerEventoDeOrganizacionPorId(Long organizacionId, Long id) {
        Evento evento = obtenerReferenciasEvento(id);
        boolean perteneceALaOrganizacion = evento.getOrganizacion() != null
                && evento.getOrganizacion().getId().equals(organizacionId);
        if (!perteneceALaOrganizacion) {
            throw new ResourceNotFoundException("Evento no encontrado con id: " + id);
        }
        return evento;
    }

    @Transactional(readOnly = true)
    public Evento obtenerReferenciasEvento(Long id) {
        return eventoRepository.findByIdConReferencias(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id: " + id));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_EVENTOS_PUBLICOS, key = "{'cat', #categoriaId, #pageable.pageNumber, #pageable.pageSize}")
    public Page<Evento> listarPorCategoria(Long categoriaId, Pageable pageable) {
        return eventoRepository.findByCategoriaVisibles(categoriaId, OrdenEventos.estable(pageable));
    }

    private static final java.time.ZoneId ZONA_EVENTHIVE = java.time.ZoneId.of("America/Bogota");

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_EVENTOS_PUBLICOS, key = "{'proximos', #pageable.pageNumber, #pageable.pageSize}")
    public Page<Evento> listarEventosProximos(Pageable pageable) {
        java.time.ZonedDateTime ahoraZona = java.time.ZonedDateTime.now(ZONA_EVENTHIVE);
        return eventoRepository.findProximosPublicados(
                ahoraZona.toLocalDate(),
                ahoraZona.toLocalTime(),
                pageable);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_EVENTOS_CARRUSEL, key = "'carrusel'")
    public List<com.eventhive.app.dto.response.EventoCarruselDTO> obtenerEventosCarrusel() {
        return eventoRepository.findCarruselPublicados().stream()
                .map(e -> {
                    String url = (e.getUrlImagenDestacado() != null && !e.getUrlImagenDestacado().isBlank())
                            ? e.getUrlImagenDestacado()
                            : e.getFoto();
                    return new com.eventhive.app.dto.response.EventoCarruselDTO(e.getId(), url);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<Evento> listarPorOrganizacion(Long organizacionId, Pageable pageable) {
        return listarPorOrganizacion(organizacionId, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Evento> listarPorOrganizacion(Long organizacionId, Long categoriaId, EstadoEvento estado, Pageable pageable) {
        return listarPorOrganizacion(organizacionId, categoriaId, estado, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Evento> listarPorOrganizacion(Long organizacionId, Long categoriaId, EstadoEvento estado, String titulo, Pageable pageable) {
        Specification<Evento> spec = (root, query, cb) -> {
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("categoria", JoinType.INNER);
                root.fetch("organizacion", JoinType.INNER);
            }
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizacion").get("id"), organizacionId));
            if (categoriaId != null) {
                predicates.add(cb.equal(root.get("categoria").get("id"), categoriaId));
            }
            if (estado != null) {
                predicates.add(cb.equal(root.get("estado"), estado));
            }
            if (titulo != null && !titulo.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("titulo")), "%" + titulo.trim().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return eventoRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public EventosResumenOrganizacionDTO obtenerResumenEventosOrganizacion(Long organizacionId) {
        long activos = eventoRepository.countActivosByOrganizacionId(organizacionId);
        long borrador = eventoRepository.countByOrganizacionIdAndEstado(organizacionId, EstadoEvento.BORRADOR);
        long finalizados = eventoRepository.countByOrganizacionIdAndEstado(organizacionId, EstadoEvento.FINALIZADO);
        long total = eventoRepository.countByOrganizacionId(organizacionId);
        return new EventosResumenOrganizacionDTO(activos, borrador, finalizados, total);
    }

    // Búsqueda pública por título
    @Transactional(readOnly = true)
    public Page<EventoDTO> buscarEventos(String titulo, Pageable pageable) {
        String tituloNormalizado = (titulo != null && !titulo.isBlank()) ? titulo.trim() : null;

        Specification<Evento> spec = (root, query, cb) -> {
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("categoria", JoinType.INNER);
                root.fetch("organizacion", JoinType.INNER);
            }
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("estado"), EstadoEvento.PUBLICADO));

            if (tituloNormalizado != null) {
                predicates.add(cb.like(cb.lower(root.get("titulo")), "%" + tituloNormalizado.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return eventoRepository.findAll(spec, OrdenEventos.promocionadosPrimero(pageable)).map(this::toDTO);
    }

    // Eventos para el mapa: filtra por categoría y/o radio de distancia
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_EVENTOS_MAPA, key = "{#categoriaId, #lat, #lng, #radioKm}")
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
    @CacheEvict(value = {CacheConfig.CACHE_EVENTOS_PUBLICOS, CacheConfig.CACHE_EVENTOS_CARRUSEL, CacheConfig.CACHE_EVENTOS_DESTACADOS, CacheConfig.CACHE_PRIMER_DESTACADO, CacheConfig.CACHE_EVENTOS_MAPA, CacheConfig.CACHE_CATEGORIAS}, allEntries = true)
    public Evento crearEvento(EventoRequest request, MultipartFile foto) {
        Usuario usuario = authHelper.usuarioAutenticado();
        Categoria categoria = resolverCategoria(request.getCategoriaId());

        if (usuario.getOrganizacion() == null) {
            throw new BusinessException("Debe pertenecer a una organización para crear eventos");
        }
        validarPermisoSobreEvento(usuario, PermisoEvento.CREAR_EVENTO);

        Organizacion organizacion = usuario.getOrganizacion();
        if (organizacion.getEstado() == EstadoOrganizacion.SUSPENDIDA) {
            throw new BusinessException("La organización está suspendida y no puede crear eventos");
        }

        validarFechasCreacion(request);

        Evento evento = new Evento();
        mapearCamposRequest(evento, request, categoria);
        evento.setOrganizacion(organizacion);
        evento.setCreadoPor(usuario);
        evento.setEstado(EstadoEvento.BORRADOR);

        if (foto != null && !foto.isEmpty()) {
            evento.setFoto(storageService.subirImagenEvento(foto));
        }

        Evento guardado = eventoRepository.save(evento);
        asegurarLocalidadGeneral(guardado);

        serviceOrganizacion.actualizarTotalEventos(organizacion.getId());

        return guardado;
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_EVENTOS_PUBLICOS, CacheConfig.CACHE_EVENTOS_CARRUSEL, CacheConfig.CACHE_EVENTOS_DESTACADOS, CacheConfig.CACHE_PRIMER_DESTACADO, CacheConfig.CACHE_EVENTOS_MAPA, CacheConfig.CACHE_CATEGORIAS}, allEntries = true)
    public Evento actualizarEvento(Long id, EventoRequest request, MultipartFile foto) {
        Evento evento = obtenerReferenciasEvento(id);
        verificarPermiso(evento, PermisoEvento.EDITAR_EVENTO);

        if (evento.getEstado() == EstadoEvento.SUSPENDIDO) {
            throw new BusinessException("El evento está suspendido y no puede modificarse");
        }
        if (evento.getEstado() == EstadoEvento.FINALIZADO) {
            throw new BusinessException("Los eventos finalizados no pueden modificarse");
        }
        if (evento.getEstado() == EstadoEvento.CANCELADO) {
            throw new BusinessException("Los eventos cancelados no pueden modificarse");
        }

        validarFechasActualizacion(evento, request);

        EstadoEvento estadoAnterior = evento.getEstado();
        Categoria categoria = resolverCategoria(request.getCategoriaId());
        mapearCamposRequest(evento, request, categoria);

        if (foto != null && !foto.isEmpty()) {
            eliminarFotoAnterior(evento.getFoto());
            evento.setFoto(storageService.subirImagenEvento(foto));
        }

        Evento guardado = eventoRepository.save(evento);
        notificarCambioSiCorresponde(guardado, estadoAnterior);
        return guardado;
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_EVENTOS_PUBLICOS, CacheConfig.CACHE_EVENTOS_CARRUSEL, CacheConfig.CACHE_EVENTOS_DESTACADOS, CacheConfig.CACHE_PRIMER_DESTACADO, CacheConfig.CACHE_EVENTOS_MAPA, CacheConfig.CACHE_CATEGORIAS}, allEntries = true)
    public void suspenderEvento(Long id) {
        Evento evento = obtenerEventoAdministrativo(id);
        verificarPermiso(evento, PermisoEvento.CANCELAR_EVENTO);

        if (evento.getEstado() != EstadoEvento.PUBLICADO) {
            throw new BusinessException("Solo se pueden suspender eventos en estado PUBLICADO");
        }

        transicionarEstado(evento, EstadoEvento.SUSPENDIDO);
        eventoRepository.save(evento);
        serviceNotification.notificarCambioEvento(evento, TipoNotification.EVENTO_SUSPENDIDO);
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_EVENTOS_PUBLICOS, CacheConfig.CACHE_EVENTOS_CARRUSEL, CacheConfig.CACHE_EVENTOS_DESTACADOS, CacheConfig.CACHE_PRIMER_DESTACADO, CacheConfig.CACHE_EVENTOS_MAPA, CacheConfig.CACHE_CATEGORIAS}, allEntries = true)
    public void enviarRevision(Long id) {
        Evento evento = obtenerEventoAdministrativo(id);
        verificarPermiso(evento, PermisoEvento.EDITAR_EVENTO);

        if (evento.getEstado() != EstadoEvento.BORRADOR
                && evento.getEstado() != EstadoEvento.EN_CORRECCION) {
            throw new BusinessException("Solo se pueden enviar a revisión eventos en BORRADOR o EN_CORRECCION");
        }

        Organizacion organizacion = evento.getOrganizacion();
        verificarLimiteDeNivel(organizacion);

        if (organizacion.getEstado() != EstadoOrganizacion.APROBADA) {
            throw new BusinessException(
                    "Tu organización debe estar aprobada (RUT verificado) antes de publicar un evento");
        }

        transicionarEstado(evento, EstadoEvento.PENDIENTE_REVISION);

        ResultadoReglaModeracion resultado = serviceModeracionAutomatica.evaluarEvento(evento);
        aplicarResultadoModeracionAutomatica(evento, resultado);

        eventoRepository.save(evento);
    }

    @Transactional
    public void reabrirEvento(Long id) {
        Evento evento = obtenerEventoAdministrativo(id);
        verificarPermiso(evento, PermisoEvento.EDITAR_EVENTO);

        if (evento.getEstado() != EstadoEvento.RECHAZADO) {
            throw new BusinessException("Solo se pueden reabrir eventos en estado RECHAZADO");
        }

        transicionarEstado(evento, EstadoEvento.BORRADOR);
        eventoRepository.save(evento);
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_EVENTOS_PUBLICOS, CacheConfig.CACHE_EVENTOS_CARRUSEL, CacheConfig.CACHE_EVENTOS_DESTACADOS, CacheConfig.CACHE_PRIMER_DESTACADO, CacheConfig.CACHE_EVENTOS_MAPA, CacheConfig.CACHE_CATEGORIAS}, allEntries = true)
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
    @CacheEvict(value = {CacheConfig.CACHE_EVENTOS_PUBLICOS, CacheConfig.CACHE_EVENTOS_CARRUSEL, CacheConfig.CACHE_EVENTOS_DESTACADOS, CacheConfig.CACHE_PRIMER_DESTACADO, CacheConfig.CACHE_EVENTOS_MAPA, CacheConfig.CACHE_CATEGORIAS}, allEntries = true)
    public void eliminarEvento(Long id) {
        Evento evento = obtenerEventoAdministrativo(id);
        verificarPermiso(evento, PermisoEvento.CANCELAR_EVENTO);

        if (evento.getEstado() == EstadoEvento.FINALIZADO) {
            throw new BusinessException("Los eventos finalizados no pueden eliminarse");
        }

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
    @CacheEvict(value = {CacheConfig.CACHE_EVENTOS_PUBLICOS, CacheConfig.CACHE_EVENTOS_CARRUSEL, CacheConfig.CACHE_EVENTOS_DESTACADOS, CacheConfig.CACHE_PRIMER_DESTACADO, CacheConfig.CACHE_EVENTOS_MAPA, CacheConfig.CACHE_CATEGORIAS}, allEntries = true)
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

    //METODOS DE MODERACION AUTOMATICA

    private void aplicarResultadoModeracionAutomatica(Evento evento, ResultadoReglaModeracion resultado) {
        Organizacion organizacion = evento.getOrganizacion();

        if (resultado.veredicto() == VeredictoModeracion.REVISION) {
            registrarModeracionAutomatica(evento, EstadoEvento.PENDIENTE_REVISION, resultado.motivo(), resultado.detalle());

            serviceNotification.notificarEventoEnviadoRevision(evento);
            return;
        }

        if (resultado.veredicto() == VeredictoModeracion.RECHAZADO) {
            transicionarEstado(evento, EstadoEvento.RECHAZADO);
            registrarModeracionAutomatica(evento, EstadoEvento.RECHAZADO, resultado.motivo(), resultado.detalle());
            incrementarRechazos(organizacion);

            serviceNotification.notificarResultadoModeracionEvento(organizacion.getRepresentante(),
                    evento, false, resultado.detalle());
            return;
        }

        // OK: solamente Nivel 3 puede publicar automáticamente.
        if (organizacion.getNivel().permitePublicacionAutomatica()) {
            transicionarEstado(evento, EstadoEvento.PUBLICADO);

            registrarModeracionAutomatica(evento, EstadoEvento.PUBLICADO,
                    null, "Aprobado automáticamente por nivel de organización ("
                            + organizacion.getNivel() + ").");

            serviceNotification.notificarNuevoEvento(evento);

            serviceNotification.notificarResultadoModeracionEvento(organizacion.getRepresentante(), evento,
                    true, null);
            return;
        }

        // Nivel 1 y 2 requieren moderación humana.
        registrarModeracionAutomatica(evento, EstadoEvento.PENDIENTE_REVISION,
                null, "Evento limpio según las reglas automáticas; "
                        + "requiere revisión humana por nivel de organización.");

        serviceNotification.notificarEventoEnviadoRevision(evento);
    }

    private void incrementarRechazos(Organizacion organizacion) {
        organizacion.setEventosRechazados(organizacion.getEventosRechazados() + 1);
        organizacionRepository.save(organizacion);
    }

    // Registra la decisión del motor de reglas.
    private void registrarModeracionAutomatica(Evento evento, EstadoEvento estadoResultante,
                                               MotivosRechazos motivo, String observacion) {
        ModeracionEvento moderacion = new ModeracionEvento();
        moderacion.setEvento(evento);
        moderacion.setModerador(null);
        moderacion.setEstadoResultante(estadoResultante);
        moderacion.setMotivo(motivo);
        moderacion.setObservacion(observacion);
        moderacion.setFecha(LocalDateTime.now());
        moderacionEventoRepository.save(moderacion);
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

    //validar IDOR que el usuario pertenezca a la organizacion del evento
    public void verificarPermiso(Evento evento, PermisoEvento permisoRequerido) {
        Usuario usuario = authHelper.usuarioAutenticado();

        boolean perteneceALaOrganizacion = usuario.getOrganizacion() != null
                && evento.getOrganizacion() != null
                && usuario.getOrganizacion().getId().equals(evento.getOrganizacion().getId());

        if (!perteneceALaOrganizacion) {
            throw new AccessDeniedException("No autorizado para modificar o acceder a este evento");
        }
        validarPermisoSobreEvento(usuario, permisoRequerido);
    }

    private void validarPermisoSobreEvento(Usuario usuario, PermisoEvento permisoRequerido) {
        Organizacion organizacion = usuario.getOrganizacion();
        if (organizacion == null)
            throw new AccessDeniedException("No perteneces a ninguna organización");

        boolean esRepresentante = organizacion.getRepresentante() != null
                && organizacion.getRepresentante().getId().equals(usuario.getId());
        if (esRepresentante) return; // el representante tiene control total

        if (usuario.getPermisosEvento() == null || !usuario.getPermisosEvento().contains(permisoRequerido))
            throw new AccessDeniedException("No tienes permiso para realizar esta acción: " + permisoRequerido);
    }

    private Categoria resolverCategoria(Long categoriaId) {
        if (categoriaId == null) {
            throw new BusinessException("El categoriaId es requerido");
        }
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + categoriaId));
    }

    private void asegurarLocalidadGeneral(Evento evento) {
        if (evento == null || localidadRepository.countByEventoId(evento.getId()) > 0) {
            return;
        }

        Localidad general = new Localidad();
        general.setEvento(evento);
        general.setNombre("General");
        general.setCapacidad(200);
        general.setDisponibles(200);
        general.setPrecio(new BigDecimal("10000"));
        localidadRepository.save(general);

        if (evento.getLocalidades() == null) {
            evento.setLocalidades(new ArrayList<>());
        }

        evento.getLocalidades().add(general);
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
        dto.setPromocionado(e.getPromocionado());
        // localidades se deja en null para listados (no se expone en listas)

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
                    e.getOrganizacion().getRazonSocial(),
                    e.getOrganizacion().getUrlLogo()));
        }

        return dto;
    }

    public EventoDTO toDetalleDTO(Evento e) {
        EventoDTO dto = toDTO(e);
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

    public PagedResponse<EventoDestacadoDTO> toPagedDestacadosDTO(Page<Evento> page) {
        return new PagedResponse<>(
                page.getContent().stream().map(evento -> {
                    EventoDTO base = toDTO(evento);
                    EventoDestacadoDTO destacado = new EventoDestacadoDTO();
                    org.springframework.beans.BeanUtils.copyProperties(base, destacado);
                    destacado.setUrlImagenDestacado(evento.getUrlImagenDestacado());
                    return destacado;
                }).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private EventoCategoriaDTO toEventoCategoriaDTO(Evento evento){
        EventoCategoriaDTO dto = new EventoCategoriaDTO();
        dto.setId(evento.getCategoria().getId());
        dto.setNombre(evento.getCategoria().getNombre());
        return dto;
    }

    private void mapearCamposRequest(Evento evento, EventoRequest req, Categoria categoria) {
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

    public void validarFechasCreacion(EventoRequest req) {
        if (req.getFecha() == null || req.getHora() == null) {
            throw new BusinessException("La fecha y la hora de inicio del evento son obligatorias");
        }

        java.time.ZonedDateTime ahoraZona = java.time.ZonedDateTime.now(ZONA_EVENTHIVE);
        LocalDate hoy = ahoraZona.toLocalDate();
        LocalTime horaActual = ahoraZona.toLocalTime();

        if (req.getFecha().isBefore(hoy)) {
            throw new BusinessException("La fecha de inicio no puede ser anterior al día actual");
        }

        if (req.getFecha().isEqual(hoy) && req.getHora().isBefore(horaActual)) {
            throw new BusinessException("Si el evento inicia hoy, la hora de inicio debe ser posterior a la hora actual");
        }

        validarFechaFin(req.getFecha(), req.getHora(), req.getFechaFin(), req.getHoraFin());
    }

    public void validarFechasActualizacion(Evento eventoExistente, EventoRequest req) {
        if (req.getFecha() == null || req.getHora() == null) {
            throw new BusinessException("La fecha y la hora de inicio del evento son obligatorias");
        }

        java.time.ZonedDateTime ahoraZona = java.time.ZonedDateTime.now(ZONA_EVENTHIVE);
        LocalDateTime ahora = ahoraZona.toLocalDateTime();
        LocalDateTime inicioExistente = LocalDateTime.of(eventoExistente.getFecha(), eventoExistente.getHora());

        boolean yaInicioOcurrio = inicioExistente.isBefore(ahora);

        if (yaInicioOcurrio) {
            boolean intentoCambiarInicio = !req.getFecha().isEqual(eventoExistente.getFecha())
                    || !req.getHora().equals(eventoExistente.getHora());
            if (intentoCambiarInicio) {
                throw new BusinessException("No se puede modificar la fecha u hora de inicio de un evento que ya inició o ha ocurrido");
            }
            validarFechaFin(eventoExistente.getFecha(), eventoExistente.getHora(), req.getFechaFin(), req.getHoraFin());
        } else {
            validarFechasCreacion(req);
        }
    }

    private void validarFechaFin(LocalDate fechaInicio, LocalTime horaInicio, LocalDate fechaFin, LocalTime horaFin) {
        if (fechaFin != null) {
            if (fechaFin.isBefore(fechaInicio)) {
                throw new BusinessException("La fecha y hora de finalización debe ser posterior a la fecha y hora de inicio");
            }
            if (fechaFin.isEqual(fechaInicio)) {
                if (horaFin != null && !horaFin.isAfter(horaInicio)) {
                    throw new BusinessException("La fecha y hora de finalización debe ser posterior a la fecha y hora de inicio");
                }
            }
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
            Set.of(EstadoEvento.BORRADOR)
    );

    @CacheEvict(value = {CacheConfig.CACHE_EVENTOS_PUBLICOS, CacheConfig.CACHE_EVENTOS_CARRUSEL, CacheConfig.CACHE_EVENTOS_DESTACADOS, CacheConfig.CACHE_PRIMER_DESTACADO, CacheConfig.CACHE_EVENTOS_MAPA, CacheConfig.CACHE_CATEGORIAS}, allEntries = true)
    public void transicionarEstado(Evento evento, EstadoEvento nuevoEstado) {
        EstadoEvento actual = evento.getEstado();

        if (!TRANSICIONES.getOrDefault(actual, Set.of()).contains(nuevoEstado)) {
            throw new BusinessException(
                    "Transición no permitida: " + actual + " -> " + nuevoEstado);
        }

        evento.setEstado(nuevoEstado);

        if (nuevoEstado == EstadoEvento.FINALIZADO) {
            evento.setPromocionado(false);
        }
    }

}
