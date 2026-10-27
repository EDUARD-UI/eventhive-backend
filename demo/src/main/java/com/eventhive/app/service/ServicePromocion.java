package com.eventhive.app.service;

import java.net.URI;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.config.CacheConfig;
import com.eventhive.app.config.SupabaseStorageConfig;
import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.enums.EstadoPosicionamiento;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.EventoRepository;
import com.eventhive.app.repository.PosicionamientoEventoRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;

import lombok.RequiredArgsConstructor;

import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ServicePromocion {

    private final EventoRepository eventoRepository;
    private final PosicionamientoEventoRepository posicionamientoRepository;
    private final AuthenticatedUserHelper authHelper;
    private final SupabaseStorageConfig storageConfig;
    private final SupabaseStorageService storageService;

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_EVENTOS_PUBLICOS, CacheConfig.CACHE_EVENTOS_CARRUSEL, CacheConfig.CACHE_EVENTOS_DESTACADOS, CacheConfig.CACHE_PRIMER_DESTACADO}, allEntries = true)
    public String subirYAsignarImagenDestacada(Long eventoId, MultipartFile imagen) {
        if (imagen == null || imagen.isEmpty()) {
            throw new BusinessException("El archivo de imagen es requerido");
        }
        Usuario usuario = authHelper.usuarioAutenticado();
        boolean esMarketingOAdmin = usuario.getRol() != null
                && ("MARKETING".equalsIgnoreCase(usuario.getRol().getNombre())
                || "ADMINISTRADOR".equalsIgnoreCase(usuario.getRol().getNombre()));
        if (!esMarketingOAdmin) {
            throw new BusinessException("Solo el personal autorizado de marketing puede asignar la imagen destacada");
        }

        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));

        if (evento.getEstado() != EstadoEvento.PUBLICADO) {
            throw new BusinessException("Solo se pueden configurar eventos publicados");
        }

        boolean tienePromocionPremium = posicionamientoRepository
                .existsByEventoIdAndEstadoAndPlanPremiumTrue(eventoId, EstadoPosicionamiento.CONFIRMADO);
        if (!tienePromocionPremium) {
            throw new BusinessException("El evento debe tener una promoción confirmada con plan premium para asignarle imagen destacada");
        }

        String url = storageService.subirImagenEvento(imagen);
        evento.setUrlImagenDestacado(url);
        eventoRepository.save(evento);
        return url;
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_EVENTOS_PUBLICOS, CacheConfig.CACHE_EVENTOS_CARRUSEL, CacheConfig.CACHE_EVENTOS_DESTACADOS, CacheConfig.CACHE_PRIMER_DESTACADO}, allEntries = true)
    public Evento asignarImagenDestacada(Long eventoId, String urlImagenDestacado) {
        Usuario usuario = authHelper.usuarioAutenticado();
        boolean esMarketingOAdmin = usuario.getRol() != null
                && ("MARKETING".equalsIgnoreCase(usuario.getRol().getNombre())
                || "ADMINISTRADOR".equalsIgnoreCase(usuario.getRol().getNombre()));
        if (!esMarketingOAdmin) {
            throw new BusinessException("Solo el personal autorizado de marketing puede asignar la imagen destacada");
        }

        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));

        if (evento.getEstado() != EstadoEvento.PUBLICADO) {
            throw new BusinessException("Solo se pueden configurar eventos publicados");
        }

        boolean tienePromocionPremium = posicionamientoRepository
                .existsByEventoIdAndEstadoAndPlanPremiumTrue(eventoId, EstadoPosicionamiento.CONFIRMADO);
        if (!tienePromocionPremium) {
            throw new BusinessException("El evento debe tener una promoción confirmada con plan premium para asignarle imagen destacada");
        }

        validarUrlImagenDestacado(urlImagenDestacado);

        evento.setUrlImagenDestacado(urlImagenDestacado.trim());
        return eventoRepository.save(evento);
    }

    private void validarUrlImagenDestacado(String url) {
        if (url == null || url.isBlank()) {
            throw new BusinessException("La URL de la imagen destacada es obligatoria");
        }
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
