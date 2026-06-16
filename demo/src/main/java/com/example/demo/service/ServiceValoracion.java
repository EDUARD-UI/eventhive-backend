package com.example.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.ValoracionDTO;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Usuario;
import com.example.demo.model.Valoracion;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.repository.ValoracionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceValoracion {

    private final ValoracionRepository valoracionRepository;
    private final UsuarioRepository    usuarioRepository;

    @Transactional(readOnly = true)
    public Page<ValoracionDTO> obtenerValoracionesDTOPorUsuario(String usuarioId, Pageable pageable) {
        return valoracionRepository.findByClienteIdConOrganizador(usuarioId, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ValoracionDTO> obtenerValoracionesDTOPorOrganizador(String organizadorId, Pageable pageable) {
        return valoracionRepository.findByOrganizadorIdConCliente(organizadorId, pageable).map(this::toDTO);
    }

    @Transactional
    @PreAuthorize("hasRole('CLIENTE')")
    public void crearValoracion(Usuario cliente, String organizadorId, String comentario, long calificacion) {
        validarCalificacion(calificacion);
        if (valoracionRepository.existsByClienteIdAndOrganizadorId(cliente.getId(), organizadorId))
            throw new BusinessException("Ya valoraste a este organizador");

        Usuario organizador = usuarioRepository.findById(organizadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizador no encontrado"));

        Valoracion v = new Valoracion();
        v.setCliente(cliente);
        v.setOrganizador(organizador);
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
        if (v.getOrganizador() != null) {
            dto.setOrganizadorId(v.getOrganizador().getId());
            dto.setOrganizadorNombre(v.getOrganizador().getNombre() + " " + v.getOrganizador().getApellido());
        }
        return dto;
    }
}