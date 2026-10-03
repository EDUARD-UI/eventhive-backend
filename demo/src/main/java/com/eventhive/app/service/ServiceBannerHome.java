package com.eventhive.app.service;

import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.eventhive.app.dto.response.BannerHomeDTO;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.BannerHome;
import com.eventhive.app.repository.BannerHomeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceBannerHome {

    private final BannerHomeRepository bannerHomeRepository;
    private final SupabaseStorageService storageService;

    @Transactional(readOnly = true)
    public List<BannerHomeDTO> obtenerBannersPublicos() {
        return bannerHomeRepository.findAllByOrderByPosicionAsc().stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BannerHomeDTO> obtenerEspaciosAdministrativos() {
        List<BannerHome> banners = bannerHomeRepository.findAllByOrderByPosicionAsc();
        return IntStream.rangeClosed(1, 2)
                .mapToObj(posicion -> banners.stream()
                        .filter(banner -> banner.getPosicion() == posicion)
                        .findFirst()
                        .map(this::toDTO)
                        .orElseGet(() -> new BannerHomeDTO(null, null, null, null, null, posicion)))
                .toList();
    }

    @Transactional
    public BannerHomeDTO crear(int posicion, String titulo, String textoBoton,
                              String enlaceUrl, MultipartFile imagen) {
        validarPosicion(posicion);
        if (bannerHomeRepository.findByPosicion(posicion).isPresent()) {
            throw new BusinessException("Ya existe un banner en la posición " + posicion);
        }
        validarContenido(titulo, textoBoton, enlaceUrl);
        if (imagen == null || imagen.isEmpty()) {
            throw new BusinessException("La imagen del banner es requerida");
        }

        BannerHome banner = new BannerHome();
        banner.setPosicion(posicion);
        banner.setTitulo(titulo.trim());
        banner.setTextoBoton(textoBoton.trim());
        banner.setEnlaceUrl(enlaceUrl.trim());
        banner.setImagenUrl(storageService.subirImagenBannerHome(imagen, posicion));

        return toDTO(bannerHomeRepository.save(banner));
    }

    @Transactional
    public BannerHomeDTO actualizar(int posicion, String titulo, String textoBoton,
                                    String enlaceUrl, MultipartFile imagen) {
        validarPosicion(posicion);
        validarContenido(titulo, textoBoton, enlaceUrl);

        BannerHome banner = bannerHomeRepository.findByPosicion(posicion)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un banner en la posición " + posicion));

        banner.setTitulo(titulo.trim());
        banner.setTextoBoton(textoBoton.trim());
        banner.setEnlaceUrl(enlaceUrl.trim());
        if (imagen != null && !imagen.isEmpty()) {
            String imagenAnterior = banner.getImagenUrl();
            banner.setImagenUrl(storageService.subirImagenBannerHome(imagen, posicion));
            storageService.eliminarImagenBannerHome(imagenAnterior);
        }

        return toDTO(bannerHomeRepository.save(banner));
    }

    @Transactional
    public void eliminar(int posicion) {
        validarPosicion(posicion);
        BannerHome banner = bannerHomeRepository.findByPosicion(posicion)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un banner en la posición " + posicion));
        bannerHomeRepository.delete(banner);
        storageService.eliminarImagenBannerHome(banner.getImagenUrl());
    }

    private void validarPosicion(int posicion) {
        if (posicion != 1 && posicion != 2) {
            throw new BusinessException("La posición debe ser 1 o 2");
        }
    }

    private void validarContenido(String titulo, String textoBoton, String enlaceUrl) {
        validarTexto(titulo, "El título es requerido");
        validarTexto(textoBoton, "El texto del botón es requerido");
        validarTexto(enlaceUrl, "El enlace es requerido");

        if (titulo.trim().length() > 150) {
            throw new BusinessException("El título no puede superar 150 caracteres");
        }
        if (textoBoton.trim().length() > 80) {
            throw new BusinessException("El texto del botón no puede superar 80 caracteres");
        }
        validarEnlace(enlaceUrl.trim());
    }

    // Solo rutas internas ("/eventos") o http(s). Bloquea esquemas como javascript: o data:
    private void validarEnlace(String enlace) {
        boolean rutaInterna = enlace.startsWith("/") && !enlace.startsWith("//");
        String minuscula = enlace.toLowerCase();
        boolean absoluta = minuscula.startsWith("https://") || minuscula.startsWith("http://");

        if ((!rutaInterna && !absoluta) || enlace.length() > 2048) {
            throw new BusinessException("El enlace debe ser una ruta interna (/eventos) o una URL http(s) válida");
        }
    }

    private void validarTexto(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new BusinessException(mensaje);
        }
    }

    private BannerHomeDTO toDTO(BannerHome banner) {
        return new BannerHomeDTO(
                banner.getId(),
                banner.getTitulo(),
                banner.getImagenUrl(),
                banner.getTextoBoton(),
                banner.getEnlaceUrl(),
                banner.getPosicion());
    }
}