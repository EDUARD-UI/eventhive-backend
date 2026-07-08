package com.eventhive.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.ModeracionEvento;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.EventoRepository;
import com.eventhive.app.repository.ModeracionEventoRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;

class ServiceModeracionTest {

    @Test
    void aprobarEventoDebePublicarYNotificarCuandoEstaEnRevision() {
        EventoRepository eventoRepository = mock(EventoRepository.class);
        ModeracionEventoRepository moderacionRepository = mock(ModeracionEventoRepository.class);
        AuthenticatedUserHelper authHelper = mock(AuthenticatedUserHelper.class);
        ServiceNotification serviceNotification = mock(ServiceNotification.class);

        ServiceModeracion service = new ServiceModeracion(eventoRepository, moderacionRepository, authHelper, serviceNotification);

        Usuario moderador = new Usuario();
        moderador.setId(10L);
        when(authHelper.usuarioAutenticado()).thenReturn(moderador);

        Evento evento = new Evento();
        evento.setId(1L);
        evento.setEstado(EstadoEvento.PENDIENTE_REVISION);
        when(eventoRepository.findByIdConReferencias(1L)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(moderacionRepository.save(any(ModeracionEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Evento resultado = service.aprobarEvento(1L);

        assertEquals(EstadoEvento.PUBLICADO, resultado.getEstado());
        verify(serviceNotification).notificarNuevoEvento(resultado);
    }
}
