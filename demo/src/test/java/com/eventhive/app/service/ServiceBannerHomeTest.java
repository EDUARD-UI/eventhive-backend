package com.eventhive.app.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.eventhive.app.dto.response.BannerHomeDTO;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.BannerHome;
import com.eventhive.app.repository.BannerHomeRepository;

@ExtendWith(MockitoExtension.class)
class ServiceBannerHomeTest {

    @Mock
    private BannerHomeRepository bannerHomeRepository;

    @Mock
    private SupabaseStorageService storageService;

    @InjectMocks
    private ServiceBannerHome serviceBannerHome;

    @Test
    void crearFallaSiPosicionInvalida() {
        MultipartFile imagen = mock(MultipartFile.class);
        assertThrows(BusinessException.class,
                () -> serviceBannerHome.crear(3, "Título", "Ver", "/destino", imagen));

        verifyNoInteractions(bannerHomeRepository, storageService);
    }

    @Test
    void crearFallaSiPosicionYaExiste() {
        MultipartFile imagen = mock(MultipartFile.class);
        when(bannerHomeRepository.findByPosicion(1)).thenReturn(Optional.of(new BannerHome()));

        assertThrows(BusinessException.class,
                () -> serviceBannerHome.crear(1, "Título", "Ver", "/destino", imagen));

        verifyNoInteractions(storageService);
    }

    @Test
    void crearFallaSiImagenEsNulaOEmpty() {
        when(bannerHomeRepository.findByPosicion(1)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> serviceBannerHome.crear(1, "Título", "Ver", "/destino", null));

        MultipartFile imagenVacia = mock(MultipartFile.class);
        when(imagenVacia.isEmpty()).thenReturn(true);
        assertThrows(BusinessException.class,
                () -> serviceBannerHome.crear(1, "Título", "Ver", "/destino", imagenVacia));

        verifyNoInteractions(storageService);
    }

    @Test
    void crearExitosoGuardaYRetornaDTO() {
        MultipartFile imagen = mock(MultipartFile.class);
        when(imagen.isEmpty()).thenReturn(false);
        when(bannerHomeRepository.findByPosicion(1)).thenReturn(Optional.empty());
        when(storageService.subirImagenBannerHome(imagen, 1)).thenReturn("https://cdn.example.com/banner-1.jpg");
        when(bannerHomeRepository.save(any(BannerHome.class))).thenAnswer(invocation -> {
            BannerHome bh = invocation.getArgument(0);
            bh.setId(10L);
            return bh;
        });

        BannerHomeDTO resultado = serviceBannerHome.crear(1, "Título Banner", "Comprar", "/compra", imagen);

        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
        assertEquals("Título Banner", resultado.getTitulo());
        assertEquals("Comprar", resultado.getTextoBoton());
        assertEquals("/compra", resultado.getEnlaceUrl());
        assertEquals("https://cdn.example.com/banner-1.jpg", resultado.getImagenUrl());
        assertEquals(1, resultado.getPosicion());
        verify(bannerHomeRepository).save(any(BannerHome.class));
        verify(storageService).subirImagenBannerHome(imagen, 1);
    }

    @Test
    void actualizarRechazaPosicionInvalida() {
        assertThrows(BusinessException.class,
                () -> serviceBannerHome.actualizar(3, "Título", "Ver", "/destino", null));

        verifyNoInteractions(bannerHomeRepository, storageService);
    }

    @Test
    void actualizarFallaSiBannerNoExiste() {
        when(bannerHomeRepository.findByPosicion(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> serviceBannerHome.actualizar(1, "Título", "Ver", "/destino", null));

        verifyNoInteractions(storageService);
    }

    @Test
    void actualizarPosicionExistenteModificaElRegistro() {
        BannerHome existente = new BannerHome();
        existente.setId(12L);
        existente.setPosicion(1);
        existente.setTitulo("Viejo título");
        when(bannerHomeRepository.findByPosicion(1)).thenReturn(Optional.of(existente));
        when(bannerHomeRepository.save(existente)).thenReturn(existente);

        BannerHomeDTO resultado = serviceBannerHome.actualizar(1, "Nuevo título", "Descubrir", "/eventos", null);

        assertEquals(12L, resultado.getId());
        assertEquals("Nuevo título", resultado.getTitulo());
        assertEquals(1, resultado.getPosicion());
        verify(bannerHomeRepository).save(existente);
        verifyNoInteractions(storageService);
    }

    @Test
    void eliminarFallaSiNoExiste() {
        when(bannerHomeRepository.findByPosicion(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> serviceBannerHome.eliminar(1));
    }

    @Test
    void eliminarExitoso() {
        BannerHome existente = new BannerHome();
        existente.setId(5L);
        existente.setPosicion(2);
        when(bannerHomeRepository.findByPosicion(2)).thenReturn(Optional.of(existente));

        serviceBannerHome.eliminar(2);

        verify(bannerHomeRepository).delete(existente);
    }
}