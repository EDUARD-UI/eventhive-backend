package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Evento;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.utils.AuthenticatedUserHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceEventosDeseados {

    private final UsuarioRepository   usuarioRepository;
    private final ServiceEvento       serviceEvento;
    private final AuthenticatedUserHelper authHelper;

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<Evento> listar() {
        Usuario u = authHelper.usuarioAutenticado();
        return u.getEventosDeseados() != null ? u.getEventosDeseados() : List.of();
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void agregar(String eventoId) {
        Usuario u = authHelper.usuarioAutenticado();
        Evento evento = serviceEvento.obtenerReferencia(eventoId);

        if (u.getEventosDeseados() == null) u.setEventosDeseados(new ArrayList<>());

        boolean yaExiste = u.getEventosDeseados().stream().anyMatch(e -> e.getId().equals(eventoId));
        if (yaExiste)
            throw new BusinessException("El evento ya está en tu lista de favoritos");

        u.getEventosDeseados().add(evento);
        usuarioRepository.save(u);
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void eliminar(String eventoId) {
        Usuario u = authHelper.usuarioAutenticado();

        if (u.getEventosDeseados() == null || u.getEventosDeseados().isEmpty())
            throw new ResourceNotFoundException("No tienes eventos en tu lista de favoritos");

        boolean existia = u.getEventosDeseados().removeIf(e -> e.getId().equals(eventoId));
        if (!existia)
            throw new ResourceNotFoundException("El evento no está en tu lista de favoritos");

        usuarioRepository.save(u);
    }
}