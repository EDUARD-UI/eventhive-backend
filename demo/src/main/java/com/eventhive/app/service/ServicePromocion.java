package com.eventhive.app.service;

import java.net.URI;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.config.SupabaseStorageConfig;
import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.EventoRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServicePromocion {

    private final EventoRepository eventoRepository;
    private final AuthenticatedUserHelper authHelper;
    private final ServiceCompra serviceCompra;
    private final SupabaseStorageConfig storageConfig;

    @Transactional
    public Evento asignarImagenDestacada(Long eventoId, String urlImagenDestacado) {
        Usuario usuario = authHelper.usuarioAutenticado();
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));

        validarPropietarioEvento(evento, usuario);
        if (evento.getEstado() != EstadoEvento.PUBLICADO) {
            throw new BusinessException("Solo se pueden configurar eventos publicados");
        }
        validarUrlImagenDestacado(urlImagenDestacado);
        serviceCompra.validarPagoPosicionamientoConfirmado(eventoId);

        evento.setUrlImagenDestacado(urlImagenDestacado.trim());
        return eventoRepository.save(evento);
    }

    private void validarPropietarioEvento(Evento evento, Usuario usuario) {
        if (evento.getOrganizacion() == null || evento.getOrganizacion().getRepresentante() == null
                || !evento.getOrganizacion().getRepresentante().getId().equals(usuario.getId())) {
            throw new ResourceNotFoundException("Evento no encontrado");
        }
    }

    private void validarUrlImagenDestacado(String url) {
        try {
            URI imagen = URI.create(url.trim());
            URI storage = URI.create(storageConfig.getUrl());
            String prefijo = "/storage/v1/object/public/" + storageConfig.getBucketEventos() + "/";
            boolean mismoOrigen = imagen.getScheme() != null
                    && imagen.getScheme().equalsIgnoreCase(storage.getScheme())
                    && imagen.getHost() != null
                    && imagen.getHost().equalsIgnoreCase(storage.getHost())
                    && imagen.getPort() == storage.getPort();
            boolean imagenValida = imagen.getPath() != null
                    && imagen.getPath().startsWith(prefijo)
                    && imagen.getPath().length() > prefijo.length();
            if (!mismoOrigen || !imagenValida || imagen.getUserInfo() != null) {
                throw new BusinessException("La imagen destacada debe pertenecer al bucket de eventos de Supabase");
            }
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("La URL de la imagen destacada no es válida");
        }
    }
}
