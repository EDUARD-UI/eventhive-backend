package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.EventoBusquedaDTO;
import com.example.demo.enums.EstadoEvento;
import com.example.demo.enums.NivelUsuario;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Evento;
import com.example.demo.model.Usuario;
import com.example.demo.repository.EventoRepository;
import com.example.demo.utils.AuthenticatedUserHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceEvento {

    private final EventoRepository    eventoRepository;
    private final AuthenticatedUserHelper authHelper;
    private final ServiceFidelizacion serviceFidelizacion;

    @Transactional(readOnly = true)
    public Page<Evento> listarTodos(Pageable pageable) {
        // Sólo eventos publicados son visibles para clientes
        Page<Evento> page = eventoRepository.findByEstadoConReferencias(EstadoEvento.PUBLICADO, pageable);
        NivelUsuario nivel = NivelUsuario.BRONCE;
        try { nivel = authHelper.usuarioAutenticado().getNivel(); } catch (Exception ex) { /* usuario no autenticado */ }
        long horas = serviceFidelizacion.obtenerHorasAnticipacion(nivel);
        List<Evento> visibles = page.getContent().stream()
                .filter(e -> {
                    LocalDateTime fechaVisible = e.getFechaPublicacion().minusHours(horas);
                    return LocalDateTime.now().isAfter(fechaVisible);
                })
                .collect(Collectors.toList());
        return new PageImpl<>(visibles, pageable, visibles.size());
    }

    @Transactional(readOnly = true)
    public Page<Evento> listarPorCategoria(String categoriaId, Pageable pageable) {
        Page<Evento> publicadosPorCategoria = eventoRepository.findByCategoriaIdConReferencias(categoriaId, pageable);
        NivelUsuario nivel = NivelUsuario.BRONCE;
        try { nivel = authHelper.usuarioAutenticado().getNivel(); } catch (Exception ex) { }
        long horas = serviceFidelizacion.obtenerHorasAnticipacion(nivel);
        List<Evento> visibles = publicadosPorCategoria.getContent().stream()
                .filter(e -> e.getEstado() == EstadoEvento.PUBLICADO)
                .filter(e -> {
                    LocalDateTime fechaVisible = e.getFechaPublicacion().minusHours(horas);
                    return LocalDateTime.now().isAfter(fechaVisible);
                })
                .collect(Collectors.toList());
        return new PageImpl<>(visibles, pageable, visibles.size());
    }

    @Transactional(readOnly = true)
    public Page<Evento> listarPorOrganizador(String organizadorId, Pageable pageable) {
        return eventoRepository.findByOrganizadorIdConReferencias(organizadorId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Evento> buscarPorOrganizadorYTitulo(String organizadorId, String titulo, Pageable pageable) {
        return eventoRepository.findByOrganizadorIdAndTituloConReferencias(organizadorId, titulo, pageable);
    }

    @Transactional(readOnly = true)
    public Page<EventoBusquedaDTO> buscarPorTitulo(String titulo, Pageable pageable) {
        // Buscar entre publicados y visibles según nivel
        Page<Evento> page = eventoRepository.findByTituloConReferencias(titulo, pageable);
        NivelUsuario nivel = NivelUsuario.BRONCE;
        try { nivel = authHelper.usuarioAutenticado().getNivel(); } catch (Exception ex) { }
        long horas = serviceFidelizacion.obtenerHorasAnticipacion(nivel);
        List<EventoBusquedaDTO> visibles = page.getContent().stream()
            .filter(e -> e.getEstado() == EstadoEvento.PUBLICADO)
            .filter(e -> {
                LocalDateTime fechaVisible = e.getFechaPublicacion().minusHours(horas);
                return LocalDateTime.now().isAfter(fechaVisible);
            })
            .map(e -> new EventoBusquedaDTO(
                e.getId(), e.getTitulo(),
                e.getCategoria() != null ? e.getCategoria().getNombre() : null))
            .collect(Collectors.toList());
        return new PageImpl<>(visibles, pageable, visibles.size());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Page<Evento> buscarAdmin(String titulo, String categoriaId, String estadoId, Pageable pageable) {
        if (titulo      != null && !titulo.isBlank())      return eventoRepository.findByTituloConReferencias(titulo.trim(), pageable);
        if (categoriaId != null && !categoriaId.isBlank()) return eventoRepository.findByCategoriaIdConReferencias(categoriaId, pageable);
        if (estadoId    != null && !estadoId.isBlank())    {
            try {
                EstadoEvento estado = EstadoEvento.valueOf(estadoId.trim());
                return eventoRepository.findByEstadoConReferencias(estado, pageable);
            } catch (Exception ex) {
                throw new BusinessException("Estado inválido: " + estadoId);
            }
        }
        return listarTodos(pageable);
    }

    @Transactional(readOnly = true)
    public Evento obtenerPorId(String id) {
        return eventoRepository.findByIdConReferencias(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id: " + id));
    }

    @Transactional
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public Evento crearEvento(Evento evento) {
        Usuario organizador = authHelper.usuarioAutenticado();
        evento.setOrganizador(organizador);
        if (evento.getFoto() != null && evento.getFoto().isBlank()) evento.setFoto(null);
        return eventoRepository.save(evento);
    }

    @Transactional
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public Evento actualizarEvento(String id, Evento datos) {
        Evento evento = obtenerPorId(id);
        verificarPermiso(evento);

        evento.setTitulo(datos.getTitulo());
        evento.setDescripcion(datos.getDescripcion());
        evento.setFecha(datos.getFecha());
        evento.setHora(datos.getHora());
        evento.setLugar(datos.getLugar());
        evento.setEstado(datos.getEstado());
        evento.setCategoria(datos.getCategoria());

        if (datos.getFoto() != null && !datos.getFoto().isBlank()) evento.setFoto(datos.getFoto());
        else if (datos.getFoto() != null)                          evento.setFoto(null);

        return eventoRepository.save(evento);
    }

    @Transactional
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMINISTRADOR')")
    public void eliminarEvento(String id) {
        Evento evento = obtenerPorId(id);
        verificarPermiso(evento);
        eventoRepository.deleteById(id);
    }

    // Usado por otros services que necesitan verificar existencia
    @Transactional(readOnly = true)
    public Evento obtenerReferencia(String id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id: " + id));
    }

    public void verificarPermiso(Evento evento) {
        Usuario u = authHelper.usuarioAutenticado();
        boolean esAdmin       = u.getRol() != null && "ADMINISTRADOR".equals(u.getRol().getNombre());
        boolean esOrganizador = evento.getOrganizador() != null
                && u.getCorreo().equals(evento.getOrganizador().getCorreo());
        if (!esAdmin && !esOrganizador)
            throw new BusinessException("No autorizado para modificar este evento");
    }
}