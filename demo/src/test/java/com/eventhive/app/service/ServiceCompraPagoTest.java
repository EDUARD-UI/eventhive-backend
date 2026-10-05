package com.eventhive.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import com.eventhive.app.enums.EstadoEvento;
import com.eventhive.app.enums.EstadoOrganizacion;
import com.eventhive.app.enums.EstadoPosicionamiento;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Organizacion;
import com.eventhive.app.model.PosicionamientoEvento;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.CompraRepository;
import com.eventhive.app.repository.EventoRepository;
import com.eventhive.app.repository.LocalidadRepository;
import com.eventhive.app.repository.PosicionamientoEventoRepository;
import com.eventhive.app.repository.PromocionRepository;
import com.eventhive.app.repository.TiqueteRepository;
import com.eventhive.app.utils.AuthenticatedUserHelper;

@ExtendWith(MockitoExtension.class)
class ServiceCompraPagoTest {

    @Mock private CompraRepository compraRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private LocalidadRepository localidadRepository;
    @Mock private PosicionamientoEventoRepository posicionamientoEventoRepository;
    @Mock private TiqueteRepository tiqueteRepository;
    @Mock private PromocionRepository promocionRepository;
    @Mock private AuthenticatedUserHelper authHelper;
    @Mock private ServiceNotification serviceNotification;
    @Mock private ServiceMonetizacion serviceMonetizacion;
    @Mock private ObjectProvider<ServiceCompra> self;
    @InjectMocks private ServiceCompra serviceCompra;

    @Test
    void registraPagoPendienteParaEventoPropioPublicado() {
        Usuario representante = representante(7L);
        Evento evento = evento(12L, representante);
        when(authHelper.usuarioAutenticado()).thenReturn(representante);
        when(eventoRepository.findById(12L)).thenReturn(Optional.of(evento));
        when(posicionamientoEventoRepository.existsByEventoIdAndEstadoIn(any(), anyList())).thenReturn(false);
        when(posicionamientoEventoRepository.save(any(PosicionamientoEvento.class))).thenAnswer(invocation -> {
            PosicionamientoEvento pago = invocation.getArgument(0);
            pago.setId(21L);
            return pago;
        });

        var resultado = serviceCompra.registrarPagoPosicionamiento(12L);

        assertEquals(EstadoPosicionamiento.PENDIENTE, resultado.getEstado());
        assertEquals(3L, resultado.getOrganizacionId());
        assertFalse(Boolean.TRUE.equals(evento.getPromocionado()));
    }

    @Test
    void confirmarPagoSimuladoNoDestacaElEventoHastaAsignarUrlSeo() {
        Usuario representante = representante(7L);
        Evento evento = evento(12L, representante);
        PosicionamientoEvento pago = new PosicionamientoEvento();
        pago.setId(21L);
        pago.setEvento(evento);
        pago.setOrganizador(representante);
        pago.setEstado(EstadoPosicionamiento.PENDIENTE);
        when(authHelper.usuarioAutenticado()).thenReturn(representante);
        when(posicionamientoEventoRepository.findById(21L)).thenReturn(Optional.of(pago));
        when(posicionamientoEventoRepository.save(pago)).thenReturn(pago);

        var resultado = serviceCompra.confirmarPagoPosicionamientoSimulado(21L);

        assertEquals(EstadoPosicionamiento.CONFIRMADO, resultado.getEstado());
        assertFalse(Boolean.TRUE.equals(evento.getPromocionado()));
        verify(posicionamientoEventoRepository).save(pago);
    }

    private Usuario representante(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        Organizacion organizacion = new Organizacion();
        organizacion.setId(3L);
        organizacion.setEstado(EstadoOrganizacion.APROBADA);
        organizacion.setRepresentante(usuario);
        usuario.setOrganizacion(organizacion);
        return usuario;
    }

    private Evento evento(Long id, Usuario representante) {
        Evento evento = new Evento();
        evento.setId(id);
        evento.setEstado(EstadoEvento.PUBLICADO);
        evento.setOrganizacion(representante.getOrganizacion());
        return evento;
    }
}