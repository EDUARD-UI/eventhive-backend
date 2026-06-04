package com.example.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.EventoBusquedaDTO;
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

    @Transactional(readOnly = true)
    public Page<Evento> listarTodos(Pageable pageable) {
        return eventoRepository.findAllConReferencias(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Evento> listarPorCategoria(String categoriaId, Pageable pageable) {
        return eventoRepository.findByCategoriaIdConReferencias(categoriaId, pageable);
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
        return eventoRepository.findByTituloConReferencias(titulo, pageable)
                .map(e -> new EventoBusquedaDTO(
                        e.getId(), e.getTitulo(),
                        e.getCategoria() != null ? e.getCategoria().getNombre() : null));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Page<Evento> buscarAdmin(String titulo, String categoriaId, String estadoId, Pageable pageable) {
        if (titulo      != null && !titulo.isBlank())      return eventoRepository.findByTituloConReferencias(titulo.trim(), pageable);
        if (categoriaId != null && !categoriaId.isBlank()) return eventoRepository.findByCategoriaIdConReferencias(categoriaId, pageable);
        if (estadoId    != null && !estadoId.isBlank())    return eventoRepository.findByEstadoIdConReferencias(estadoId, pageable);
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