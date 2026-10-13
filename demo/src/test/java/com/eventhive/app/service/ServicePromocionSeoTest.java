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

import com.eventhive.app.config.SupabaseStorageConfig;
import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.enums.EstadoPosicionamiento;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.Rol;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.EventoRepository;
import com.eventhive.app.repository.PosicionamientoEventoRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;

@ExtendWith(MockitoExtension.class)
class ServicePromocionSeoTest {

    @Mock private EventoRepository eventoRepository;
    @Mock private PosicionamientoEventoRepository posicionamientoRepository;
    @Mock private AuthenticatedUserHelper authHelper;
    @Mock private SupabaseStorageConfig storageConfig;
    @InjectMocks private ServicePromocion servicePromocion;

    @Test
    void asignarImagenConservaEventoDestacadoTrasPagoConfirmado() {
        Usuario marketing = new Usuario();
        marketing.setId(8L);
        Rol rolMarketing = new Rol();
        rolMarketing.setNombre("MARKETING");
        marketing.setRol(rolMarketing);

        Organizacion organizacion = new Organizacion();
        organizacion.setRepresentante(marketing);
        Evento evento = new Evento();
        evento.setId(17L);
        evento.setEstado(EstadoEvento.PUBLICADO);
        evento.setPromocionado(true);
        evento.setOrganizacion(organizacion);

        when(authHelper.usuarioAutenticado()).thenReturn(marketing);
        when(storageConfig.getUrl()).thenReturn("https://project.supabase.co");
        when(storageConfig.getBucketEventos()).thenReturn("eventos-images");
        when(eventoRepository.findById(17L)).thenReturn(Optional.of(evento));
        when(posicionamientoRepository.existsByEventoIdAndEstadoAndPlanPremiumTrue(17L, EstadoPosicionamiento.CONFIRMADO))
                .thenReturn(true);
        when(eventoRepository.save(evento)).thenReturn(evento);

        String imageUrl = "https://project.supabase.co/storage/v1/object/public/eventos-images/featured.jpg";
        Evento resultado = servicePromocion.asignarImagenDestacada(17L, imageUrl);

        assertTrue(resultado.getPromocionado());
        assertEquals(imageUrl, resultado.getUrlImagenDestacado());
    }
}