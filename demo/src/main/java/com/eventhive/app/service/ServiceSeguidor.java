package com.eventhive.app.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.dto.response.OrganizacionDTO;
import com.eventhive.app.dto.response.SeguidorDTO;
import com.eventhive.app.dto.response.UsuarioDTO;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.Seguidor;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.OrganizacionRepository;
import com.eventhive.app.repository.SeguidorRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceSeguidor {

    private final ServiceOrganizacion serviceOrganizacion;
    private final SeguidorRepository seguidorRepository;
    private final OrganizacionRepository organizacionRepository;
    private final AuthenticatedUserHelper authHelper;

    //CONSULTAS
    @Transactional
    public void seguir(Long organizacionId) {
        Usuario seguidor = authHelper.usuarioAutenticado();
        Organizacion organizacion = buscarOrganizacion(organizacionId);

        if (seguidor.getOrganizacion() != null
                && seguidor.getOrganizacion().getId().equals(organizacionId))
            throw new BusinessException("No puedes seguir tu propia organización");

        if (seguidorRepository.existsByOrganizacionIdAndSeguidorId(organizacionId, seguidor.getId()))
            throw new BusinessException("Ya sigues a esta organización");

        Seguidor relacion = new Seguidor();
        relacion.setOrganizacion(organizacion);
        relacion.setSeguidor(seguidor);
        seguidorRepository.save(relacion);
        serviceOrganizacion.actualizarTotalSeguidores(organizacionId);
    }

    @Transactional
    public void dejarDeSeguir(Long organizacionId) {
        Usuario seguidor = authHelper.usuarioAutenticado();

        if (!seguidorRepository.existsByOrganizacionIdAndSeguidorId(organizacionId, seguidor.getId()))
            throw new ResourceNotFoundException("No sigues a esta organización");

        seguidorRepository.deleteByOrganizacionIdAndSeguidorId(organizacionId, seguidor.getId());
        serviceOrganizacion.actualizarTotalSeguidores(organizacionId);
    }

    // listar seguidores de una organizacion
    public Page<UsuarioDTO> listarSeguidores(Long organizacionId, Pageable pageable) {
        return seguidorRepository
                .findSeguidoresByOrganizacionId(organizacionId, pageable)
                .map(this::toUsuarioDTO);
    }

    @Transactional(readOnly = true)
    public Page<OrganizacionDTO> listarOrganizacionesSeguidas(Pageable pageable) {
        Usuario seguidor = authHelper.usuarioAutenticado();
        return seguidorRepository.findOrganizacionesBySeguidorId(seguidor.getId(), pageable)
                .map(serviceOrganizacion::toDTO);
    }

    //METODOS AUXILIARES Y MAPEO
    private Organizacion buscarOrganizacion(Long organizacionId) {
        return organizacionRepository.findById(organizacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Organización no encontrada: " + organizacionId));
    }

    public SeguidorDTO toDTO(Seguidor seguidor){
        SeguidorDTO dto = new SeguidorDTO();
        dto.setId(seguidor.getId());
        dto.setNombre(seguidor.getSeguidor().getNombreCompleto());
        dto.setFechaSeguimiento(seguidor.getFechaSeguimiento());
        return dto;
    }

    private UsuarioDTO toUsuarioDTO(Usuario usuario) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(usuario.getId());
        dto.setNombre(usuario.getNombreCompleto());
        dto.setCorreo(usuario.getCorreo());
        dto.setTelefono(usuario.getTelefono());
        if (usuario.getRol() != null) {
            dto.setRolNombre(usuario.getRol().getNombre());
        }
        return dto;
    }
}