package com.eventhive.app.service;

import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.Seguidor;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.OrganizacionRepository;
import com.eventhive.app.repository.SeguidorRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceSeguidor {

    private final ServiceOrganizacion serviceOrganizacion;
    private final SeguidorRepository seguidorRepository;
    private final OrganizacionRepository organizacionRepository;
    private final AuthenticatedUserHelper authHelper;

    @Transactional
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("isAuthenticated()")
    public void dejarDeSeguir(Long organizacionId) {
        Usuario seguidor = authHelper.usuarioAutenticado();

        if (!seguidorRepository.existsByOrganizacionIdAndSeguidorId(organizacionId, seguidor.getId()))
            throw new ResourceNotFoundException("No sigues a esta organización");

        seguidorRepository.deleteByOrganizacionIdAndSeguidorId(organizacionId, seguidor.getId());
        serviceOrganizacion.actualizarTotalSeguidores(organizacionId);
    }

    // listar seguidores de una organizacion
    @Transactional(readOnly = true)
    public Page<Usuario> listarSeguidores(Long organizacionId, Pageable pageable) {
        buscarOrganizacion(organizacionId);
        return seguidorRepository.findSeguidoresByOrganizacionId(organizacionId, pageable);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<Organizacion> listarOrganizacionesSeguidas(Pageable pageable) {
        Usuario seguidor = authHelper.usuarioAutenticado();
        return seguidorRepository.findOrganizacionesBySeguidorId(seguidor.getId(), pageable);
    }

    private Organizacion buscarOrganizacion(Long organizacionId) {
        return organizacionRepository.findById(organizacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Organización no encontrada: " + organizacionId));
    }
}