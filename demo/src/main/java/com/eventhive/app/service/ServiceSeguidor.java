package com.eventhive.app.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Seguidor;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.SeguidorRepository;
import com.eventhive.app.repository.UsuarioRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceSeguidor {

    private final SeguidorRepository     seguidorRepository;
    private final UsuarioRepository      usuarioRepository;
    private final AuthenticatedUserHelper authHelper;

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void seguir(Long organizadorId) {
        Usuario seguidor    = authHelper.usuarioAutenticado();
        Usuario organizador = buscarOrganizador(organizadorId);

        if (seguidor.getId().equals(organizadorId))
            throw new BusinessException("No puedes seguirte a ti mismo");

        if (seguidorRepository.existsByOrganizadorIdAndSeguidorId(organizadorId, seguidor.getId()))
            throw new BusinessException("Ya sigues a este organizador");

        Seguidor relacion = new Seguidor();
        relacion.setOrganizador(organizador);
        relacion.setSeguidor(seguidor);
        seguidorRepository.save(relacion);
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void dejarDeSeguir(Long organizadorId) {
        Usuario seguidor = authHelper.usuarioAutenticado();

        if (!seguidorRepository.existsByOrganizadorIdAndSeguidorId(organizadorId, seguidor.getId()))
            throw new ResourceNotFoundException("No sigues a este organizador");

        seguidorRepository.deleteByOrganizadorIdAndSeguidorId(organizadorId, seguidor.getId());
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarSeguidores(Long organizadorId) {
        buscarOrganizador(organizadorId);
        return seguidorRepository.findSeguidoresByOrganizadorId(organizadorId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<Usuario> listarSiguiendo() {
        Usuario seguidor = authHelper.usuarioAutenticado();
        return seguidorRepository.findOrganizadoresBySeguidorId(seguidor.getId());
    }

    @Transactional(readOnly = true)
    public long contarSeguidores(Long organizadorId) {
        return seguidorRepository.countByOrganizadorId(organizadorId);
    }

    // metodo auxiliar
    private Usuario buscarOrganizador(Long organizadorId) {
        return usuarioRepository.findById(organizadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizador no encontrado: " + organizadorId));
    }
}