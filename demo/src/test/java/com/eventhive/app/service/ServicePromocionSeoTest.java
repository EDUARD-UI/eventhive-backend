package com.eventhive.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.config.SupabaseStorageConfig;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.EventoRepository;
import com.eventhive.app.repository.PromocionRepository;
import com.eventhive.app.repository.TiqueteRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;

@ExtendWith(MockitoExtension.class)
class ServicePromocionSeoTest {

    @Mock private PromocionRepository promocionRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private TiqueteRepository tiqueteRepository;
    @Mock private AuthenticatedUserHelper authHelper;
    @Mock private ServiceCompra serviceCompra;
    @Mock private SupabaseStorageConfig storageConfig;
    @InjectMocks private ServicePromocion servicePromocion;

    @Test
    void posicionarGuardaUrlYActivaEventoSoloTrasPagoConfirmado() {
        Usuario representante = new Usuario();
        representante.setId(8L);
        Organizacion organizacion = new Organizacion();
        organizacion.setRepresentante(representante);
        Evento evento = new Evento();
        evento.setId(17L);
        evento.setEstado(EstadoEvento.PUBLICADO);
        evento.setOrganizacion(organizacion);

        when(authHelper.usuarioAutenticado()).thenReturn(representante);
        when(storageConfig.getUrl()).thenReturn("https://project.supabase.co");
        when(storageConfig.getBucketEventos()).thenReturn("eventos-images");
        when(eventoRepository.findById(17L)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(evento)).thenReturn(evento);

        String imageUrl = "https://project.supabase.co/storage/v1/object/public/eventos-images/featured.jpg";
        Evento resultado = servicePromocion.posicionarEvento(17L, imageUrl);

        assertTrue(resultado.getPromocionado());
        assertEquals(imageUrl, resultado.getUrlImagenDestacado());
        verify(serviceCompra).validarPagoPosicionamientoConfirmado(17L);
    }
}