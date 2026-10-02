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
        validarTexto(titulo, "El título es requerido");
        validarTexto(textoBoton, "El texto del botón es requerido");
        validarTexto(enlaceUrl, "El enlace es requerido");
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
        validarTexto(titulo, "El título es requerido");
        validarTexto(textoBoton, "El texto del botón es requerido");
        validarTexto(enlaceUrl, "El enlace es requerido");

        BannerHome banner = bannerHomeRepository.findByPosicion(posicion)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un banner en la posición " + posicion));

        banner.setTitulo(titulo.trim());
        banner.setTextoBoton(textoBoton.trim());
        banner.setEnlaceUrl(enlaceUrl.trim());
        if (imagen != null && !imagen.isEmpty()) {
            banner.setImagenUrl(storageService.subirImagenBannerHome(imagen, posicion));
        }

        return toDTO(bannerHomeRepository.save(banner));
    }

    @Transactional
    public void eliminar(int posicion) {
        validarPosicion(posicion);
        BannerHome banner = bannerHomeRepository.findByPosicion(posicion)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un banner en la posición " + posicion));
        bannerHomeRepository.delete(banner);
    }

    private void validarPosicion(int posicion) {
        if (posicion != 1 && posicion != 2) {
            throw new BusinessException("La posición debe ser 1 o 2");
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