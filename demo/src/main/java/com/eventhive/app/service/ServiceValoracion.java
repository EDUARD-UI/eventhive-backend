package com.eventhive.app.service;

import com.eventhive.app.dto.response.ValoracionDTO;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.model.Valoracion;
import com.eventhive.app.repository.OrganizacionRepository;
import com.eventhive.app.repository.UsuarioRepository;
import com.eventhive.app.repository.ValoracionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceValoracion {

    private final ValoracionRepository valoracionRepository;
    private final OrganizacionRepository organizacionRepository;
    private final ServiceOrganizacion serviceOrganizacion;

    @Transactional(readOnly = true)
    public Page<ValoracionDTO> obtenerValoracionesDTOPorUsuario(Long usuarioId, Pageable pageable) {
        return valoracionRepository.findByClienteIdConOrganizacion(usuarioId, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ValoracionDTO> obtenerValoracionesDTOPorOrganizacion(Long organizacionId, Pageable pageable) {
        return valoracionRepository.findByOrganizacionIdConCliente(organizacionId, pageable).map(this::toDTO);
    }

    @Transactional
    @PreAuthorize("hasRole('CLIENTE')")
    public void crearValoracion(Usuario cliente, Long organizacionId, String comentario, long calificacion) {
        validarNoEsMismoUsuario(cliente.getId(), organizacionId);
        validarCalificacion(calificacion);

        if (valoracionRepository.existsByClienteIdAndOrganizacionId(cliente.getId(), organizacionId)) {
            throw new BusinessException("Ya valoraste a este organizador");
        }

        Organizacion organizacion = organizacionRepository.findById(organizacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Organización no encontrada"));

        Valoracion v = new Valoracion();
        v.setCliente(cliente);
        v.setOrganizacion(organizacion);
        v.setComentario(comentario);
        v.setCalificacion((int) calificacion);
        valoracionRepository.save(v);

        serviceOrganizacion.actualizarMetricasValoracion(organizacionId);
    }

    @Transactional
    @PreAuthorize("hasRole('CLIENTE')")
    public void actualizarValoracion(Long id, Usuario cliente, String comentario, long calificacion) {
        validarCalificacion(calificacion);
        Valoracion v = obtenerVerificada(id, cliente);
        Long organizacionId = v.getOrganizacion().getId();

        v.setComentario(comentario);
        v.setCalificacion((int) calificacion);
        valoracionRepository.save(v);

        serviceOrganizacion.actualizarMetricasValoracion(organizacionId);
    }

    @Transactional
    @PreAuthorize("hasRole('CLIENTE')")
    public void eliminarValoracion(Long id, Usuario cliente) {
        Valoracion v = obtenerVerificada(id, cliente);
        Long organizacionId = v.getOrganizacion().getId();

        valoracionRepository.deleteById(id);

        serviceOrganizacion.actualizarMetricasValoracion(organizacionId);
    }

    // METODOS AUXILIARES Y DE MAPEO
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

    private void validarNoEsMismoUsuario(Long clienteId, Long organizacionId) {
        if (clienteId.equals(organizacionId)) {
            throw new BusinessException("No puedes valorarte a ti mismo");
        }
    }

    private ValoracionDTO toDTO(Valoracion v) {
        ValoracionDTO dto = new ValoracionDTO();
        dto.setId(v.getId());
        dto.setComentario(v.getComentario());
        dto.setCalificacion(v.getCalificacion());

        if (v.getCliente() != null) {
            dto.setClienteId(v.getCliente().getId());
            dto.setClienteNombre(v.getCliente().getNombreCompleto());
        }

        if (v.getOrganizacion() != null) {
            dto.setOrganizacionId(v.getOrganizacion().getId());
            dto.setOrganizacionNombre(v.getOrganizacion().getRazonSocial());
        }
        return dto;
    }
}
