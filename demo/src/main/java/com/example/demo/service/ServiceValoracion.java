package com.example.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.ValoracionDTO;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Evento;
import com.example.demo.model.Usuario;
import com.example.demo.model.Valoracion;
import com.example.demo.repository.EventoRepository;
import com.example.demo.repository.ValoracionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceValoracion {

    private final ValoracionRepository valoracionRepository;
    private final EventoRepository     eventoRepository;

    @Transactional(readOnly = true)
    public Page<ValoracionDTO> obtenerValoracionesDTOPorUsuario(String usuarioId, Pageable pageable) {
        return valoracionRepository.findByClienteIdConEvento(usuarioId, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ValoracionDTO> obtenerValoracionesDTOPorEvento(String eventoId, Pageable pageable) {
        return valoracionRepository.findByEventoIdConCliente(eventoId, pageable).map(this::toDTO);
    }

    @Transactional
    @PreAuthorize("hasRole('CLIENTE')")
    public void crearValoracion(Usuario cliente, String eventoId, String comentario, long calificacion) {
        validarCalificacion(calificacion);

        if (valoracionRepository.existsByClienteIdAndEventoId(cliente.getId(), eventoId))
            throw new BusinessException("Ya valoraste este evento");

        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));

        Valoracion v = new Valoracion();
        v.setCliente(cliente);
        v.setEvento(evento);
        v.setComentario(comentario);
        v.setCalificacion((int) calificacion);
        valoracionRepository.save(v);
    }

    @Transactional
    @PreAuthorize("hasRole('CLIENTE')")
    public void actualizarValoracion(String id, Usuario cliente, String comentario, long calificacion) {
        validarCalificacion(calificacion);
        Valoracion v = obtenerVerificada(id, cliente);
        v.setComentario(comentario);
        v.setCalificacion((int) calificacion);
        valoracionRepository.save(v);
    }

    @Transactional
    @PreAuthorize("hasRole('CLIENTE')")
    public void eliminarValoracion(String id, Usuario cliente) {
        obtenerVerificada(id, cliente);
        valoracionRepository.deleteById(id);
    }

    // --- helpers ---

    private Valoracion obtenerVerificada(String id, Usuario cliente) {
        Valoracion v = valoracionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Valoración no encontrada"));
        if (!v.getCliente().getId().equals(cliente.getId()))
            throw new BusinessException("No autorizado para modificar esta valoración");
        return v;
    }

    private void validarCalificacion(long c) {
        if (c < 1 || c > 5) throw new BusinessException("La calificación debe estar entre 1 y 5");
    }

    private ValoracionDTO toDTO(Valoracion v) {
        ValoracionDTO dto = new ValoracionDTO();
        dto.setId(v.getId());
        dto.setComentario(v.getComentario());
        dto.setCalificacion(v.getCalificacion());
        if (v.getEvento() != null) {
            dto.setEventoId(v.getEvento().getId());
            dto.setEventoTitulo(v.getEvento().getTitulo());
        }
        return dto;
    }
}