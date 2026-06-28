package com.eventhive.app.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.dto.ValoracionDTO;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.model.Valoracion;
import com.eventhive.app.repository.UsuarioRepository;
import com.eventhive.app.repository.ValoracionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceValoracion {

    private final ValoracionRepository valoracionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServiceMetricasOrganizador serviceMetricasOrganizador;   // ← inyección nueva

    @Transactional(readOnly = true)
    public Page<ValoracionDTO> obtenerValoracionesDTOPorUsuario(Long usuarioId, Pageable pageable) {
        return valoracionRepository.findByClienteIdConOrganizador(usuarioId, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ValoracionDTO> obtenerValoracionesDTOPorOrganizador(Long organizadorId, Pageable pageable) {
        return valoracionRepository.findByOrganizadorIdConCliente(organizadorId, pageable).map(this::toDTO);
    }

    @Transactional
    @PreAuthorize("hasRole('CLIENTE')")
    public void crearValoracion(Usuario cliente, Long organizadorId, String comentario, long calificacion) {
        validarNoEsMismoUsuario(cliente.getId(), organizadorId);
        validarCalificacion(calificacion);

        if (valoracionRepository.existsByClienteIdAndOrganizadorId(cliente.getId(), organizadorId)) {
            throw new BusinessException("Ya valoraste a este organizador");
        }

        Usuario organizador = usuarioRepository.findById(organizadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizador no encontrado"));

        Valoracion v = new Valoracion();
        v.setCliente(cliente);
        v.setOrganizador(organizador);
        v.setComentario(comentario);
        v.setCalificacion((int) calificacion);
        valoracionRepository.save(v);

        serviceMetricasOrganizador.actualizarMetricasValoracion(organizadorId);
    }

    @Transactional
    @PreAuthorize("hasRole('CLIENTE')")
    public void actualizarValoracion(Long id, Usuario cliente, String comentario, long calificacion) {
        validarCalificacion(calificacion);
        Valoracion v = obtenerVerificada(id, cliente);
        Long organizadorId = v.getOrganizador().getId();

        v.setComentario(comentario);
        v.setCalificacion((int) calificacion);
        valoracionRepository.save(v);

        serviceMetricasOrganizador.actualizarMetricasValoracion(organizadorId);
    }

    @Transactional
    @PreAuthorize("hasRole('CLIENTE')")
    public void eliminarValoracion(Long id, Usuario cliente) {
        Valoracion v = obtenerVerificada(id, cliente);
        Long organizadorId = v.getOrganizador().getId();

        valoracionRepository.deleteById(id);

        serviceMetricasOrganizador.actualizarMetricasValoracion(organizadorId);
    }

    // --- helpers ---
    private Valoracion obtenerVerificada(Long id, Usuario cliente) {
        Valoracion v = valoracionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Valoración no encontrada"));
        if (!v.getCliente().getId().equals(cliente.getId())) {
            throw new BusinessException("No autorizado para modificar esta valoración");
        }
        return v;
    }

    private void validarCalificacion(long c) {
        if (c < 1 || c > 5) {
            throw new BusinessException("La calificación debe estar entre 1 y 5");
        }
    }

    private void validarNoEsMismoUsuario(Long clienteId, Long organizadorId) {
        if (clienteId.equals(organizadorId)) {
            throw new BusinessException("No puedes valorarte a ti mismo");
        }
    }

    private ValoracionDTO toDTO(Valoracion v) {
        ValoracionDTO dto = new ValoracionDTO();
        dto.setId(v.getId());
        dto.setComentario(v.getComentario());
        dto.setCalificacion(v.getCalificacion());
        if (v.getOrganizador() != null) {
            dto.setOrganizadorId(v.getOrganizador().getId());
            dto.setOrganizadorNombre(v.getOrganizador().getNombreCompleto());
        }
        if (v.getCliente() != null) {
            dto.setClienteId(v.getCliente().getId());
            dto.setClienteNombre(v.getCliente().getNombreCompleto());
        }
        return dto;
    }
}
