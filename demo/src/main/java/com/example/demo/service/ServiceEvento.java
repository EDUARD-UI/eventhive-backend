package com.example.demo.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.config.SupabaseStorageConfig;
import com.example.demo.dto.EventoBusquedaDTO;
import com.example.demo.dto.EventoCategoriaDTO;
import com.example.demo.dto.EventoDTO;
import com.example.demo.dto.EventoOrganizadorDTO;
import com.example.demo.dto.EventoRequest;
import com.example.demo.dto.PagedResponse;
import com.example.demo.enums.EstadoEvento;
import com.example.demo.enums.NivelUsuario;
import com.example.demo.enums.TipoNotification;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Categoria;
import com.example.demo.model.Evento;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.EventoRepository;
import com.example.demo.utils.AuthenticatedUserHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceEvento {

    private final EventoRepository        eventoRepository;
    private final CategoriaRepository     categoriaRepository;
    private final AuthenticatedUserHelper authHelper;
    private final ServiceFidelizacion     serviceFidelizacion;
    private final ServiceNotification     serviceNotification;
    private final SupabaseStorageService  storageService;
    private final SupabaseStorageConfig   storageConfig;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Consultas

    @Transactional(readOnly = true)
    public Page<Evento> listarTodos(Pageable pageable) {
        Page<Evento> page = eventoRepository.findByEstadoConReferencias(EstadoEvento.PUBLICADO, pageable);
        long horas = obtenerHorasNivel();
        List<Evento> visibles = page.getContent().stream()
                .filter(e -> esVisible(e, horas))
                .collect(Collectors.toList());
        return new PageImpl<>(visibles, pageable, visibles.size());
    }

    @Transactional(readOnly = true)
    public Page<Evento> listarPorCategoria(Long categoriaId, Pageable pageable) {
        Page<Evento> page = eventoRepository.findByCategoriaIdConReferencias(categoriaId, pageable);
        long horas = obtenerHorasNivel();
        List<Evento> visibles = page.getContent().stream()
                .filter(e -> e.getEstado() == EstadoEvento.PUBLICADO)
                .filter(e -> esVisible(e, horas))
                .collect(Collectors.toList());
        return new PageImpl<>(visibles, pageable, visibles.size());
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
        Page<Evento> page = eventoRepository.findByTituloConReferencias(titulo, pageable);
        long horas = obtenerHorasNivel();
        List<EventoBusquedaDTO> visibles = page.getContent().stream()
                .filter(e -> e.getEstado() == EstadoEvento.PUBLICADO)
                .filter(e -> esVisible(e, horas))
                .map(e -> new EventoBusquedaDTO(
                        e.getId(),
                        e.getTitulo(),
                        e.getCategoria() != null ? e.getCategoria().getNombre() : null))
                .collect(Collectors.toList());
        return new PageImpl<>(visibles, pageable, visibles.size());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Page<Evento> buscarAdmin(String titulo, Long categoriaId, String estado, Pageable pageable) {
        if (titulo != null && !titulo.isBlank())
            return eventoRepository.findByTituloConReferencias(titulo.trim(), pageable);
        if (categoriaId != null)
            return eventoRepository.findByCategoriaIdConReferencias(categoriaId, pageable);
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
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public Evento crearEvento(EventoRequest request, MultipartFile foto) {
        Usuario organizador = authHelper.usuarioAutenticado();
        Categoria categoria = resolverCategoria(request.getCategoriaId());

        Evento evento = new Evento();
        mapearCampos(evento, request, categoria);
        evento.setOrganizador(organizador);

        if (foto != null && !foto.isEmpty())
            evento.setFoto(storageService.subirImagenEvento(foto));

        Evento guardado = eventoRepository.save(evento);

        if (guardado.getEstado() == EstadoEvento.PUBLICADO)
            serviceNotification.notificarNuevoEvento(guardado);

        return guardado;
    }

    @Transactional
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public Evento actualizarEvento(Long id, EventoRequest request, MultipartFile foto) {
        Evento evento = obtenerPorId(id);
        verificarPermiso(evento);

        EstadoEvento estadoAnterior = evento.getEstado();
        Categoria categoria = resolverCategoria(request.getCategoriaId());
        mapearCampos(evento, request, categoria);

        if (foto != null && !foto.isEmpty()) {
            eliminarFotoAnterior(evento.getFoto());
            evento.setFoto(storageService.subirImagenEvento(foto));
        }

        Evento guardado = eventoRepository.save(evento);

        notificarCambioSiCorresponde(guardado, estadoAnterior);

        return guardado;
    }

    @Transactional
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public void eliminarEvento(Long id) {
        Evento evento = obtenerPorId(id);
        verificarPermiso(evento);
        eliminarFotoAnterior(evento.getFoto());
        eventoRepository.deleteById(id);
    }

    // Permisos

    public void verificarPermiso(Evento evento) {
        Usuario u      = authHelper.usuarioAutenticado();
        boolean esAdmin = u.getRol() != null && "ADMINISTRADOR".equals(u.getRol().getNombre());
        boolean esOwner = evento.getOrganizador() != null
                && u.getCorreo().equals(evento.getOrganizador().getCorreo());
        if (!esAdmin && !esOwner)
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
                    e.getCategoria().getId(), e.getCategoria().getNombre()));

        if (e.getOrganizador() != null)
            dto.setOrganizador(new EventoOrganizadorDTO(
                    e.getOrganizador().getId(),
                    e.getOrganizador().getNombreCompleto(),
                    e.getOrganizador().getEsVerificado()));

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

    // metodos auxiliares

    private void mapearCampos(Evento evento, EventoRequest req, Categoria categoria) {
        evento.setTitulo(req.getTitulo());
        evento.setDescripcion(req.getDescripcion());
        evento.setFecha(req.getFecha());
        evento.setHora(req.getHora());
        evento.setLugar(req.getLugar());
        evento.setLatitud(req.getLatitud());
        evento.setLongitud(req.getLongitud());
        evento.setCategoria(categoria);

        if (req.getEstado() != null)
            evento.setEstado(req.getEstado());

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

    private boolean esVisible(Evento e, long horasAnticipacion) {
        LocalDateTime fechaVisible = e.getFechaPublicacion().minusHours(horasAnticipacion);
        return LocalDateTime.now().isAfter(fechaVisible);
    }

    private long obtenerHorasNivel() {
        NivelUsuario nivel = NivelUsuario.BRONCE;
        try {
            nivel = authHelper.usuarioAutenticado().getNivel();
        } catch (Exception ex) { /* usuario no autenticado */ }
        return serviceFidelizacion.obtenerHorasAnticipacion(nivel);
    }

    private void notificarCambioSiCorresponde(Evento guardado, EstadoEvento estadoAnterior) {
        EstadoEvento estadoActual = guardado.getEstado();

        if (estadoActual == EstadoEvento.CANCELADO) {
            serviceNotification.notificarCambioEvento(guardado, TipoNotification.EVENTO_CANCELADO);
            return;
        }

        boolean estabaPublicado = estadoAnterior == EstadoEvento.PUBLICADO;
        boolean siguePublicado  = estadoActual  == EstadoEvento.PUBLICADO;

        if (estabaPublicado && siguePublicado)
            serviceNotification.notificarCambioEvento(guardado, TipoNotification.EVENTO_MODIFICADO);
    }
}