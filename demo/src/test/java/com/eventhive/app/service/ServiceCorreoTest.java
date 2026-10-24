package com.eventhive.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventhive.app.model.Compra;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Localidad;
import com.eventhive.app.model.Tiquete;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.TiqueteRepository;
import com.eventhive.app.utils.CorreoPlantillas;
import com.eventhive.app.utils.CorreoSender;
import com.eventhive.app.utils.GeneradorCodigoQR;

@ExtendWith(MockitoExtension.class)
class ServiceCorreoTest {

    @Mock private CorreoSender sender;
    @Mock private TiqueteRepository tiqueteRepository;

    @Test
    void enviaUnQrEnLineaPorCadaBoletoIncluidoEnElCorreo() {
        CorreoPlantillas plantillas = new CorreoPlantillas();
        ServiceCorreo service = new ServiceCorreo(
                sender, plantillas, tiqueteRepository, new GeneradorCodigoQR());

        Usuario cliente = org.mockito.Mockito.mock(Usuario.class);
        Compra compra = org.mockito.Mockito.mock(Compra.class);
        Evento evento = org.mockito.Mockito.mock(Evento.class);
        Localidad localidad = org.mockito.Mockito.mock(Localidad.class);
        Tiquete primerTiquete = tiquete("codigo-boleto-1", evento, localidad);
        Tiquete segundoTiquete = tiquete("codigo-boleto-2", evento, localidad);

        when(compra.getCliente()).thenReturn(cliente);
        when(compra.getId()).thenReturn(42L);
        when(cliente.getCorreo()).thenReturn("persona@example.com");
        when(cliente.getNombreCompleto()).thenReturn("Ana");
        when(tiqueteRepository.findByCompraIdConDetalles(42L))
                .thenReturn(List.of(primerTiquete, segundoTiquete));
        when(evento.getTitulo()).thenReturn("Concierto");
        when(evento.getFecha()).thenReturn(LocalDate.of(2026, 10, 24));
        when(evento.getHora()).thenReturn(LocalTime.of(20, 0));
        when(evento.getLugar()).thenReturn("Auditorio");
        when(localidad.getNombre()).thenReturn("General");

        service.enviarBoletos(compra);

        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, byte[]>> imagenes = ArgumentCaptor.forClass(Map.class);
        verify(sender).enviarAsync(
                eq("persona@example.com"), eq("Tus boletos — EventHive"), html.capture(), imagenes.capture());

        assertTrue(html.getValue().contains("cid:qr-boleto-0"));
        assertTrue(html.getValue().contains("cid:qr-boleto-1"));
        assertEquals(2, imagenes.getValue().size());
        assertNotNull(imagenes.getValue().get("qr-boleto-0"));
        assertNotNull(imagenes.getValue().get("qr-boleto-1"));
        assertTrue(imagenes.getValue().get("qr-boleto-0").length > 0);
        assertTrue(imagenes.getValue().get("qr-boleto-1").length > 0);
    }

    private Tiquete tiquete(String codigo, Evento evento, Localidad localidad) {
        Tiquete tiquete = org.mockito.Mockito.mock(Tiquete.class);
        when(tiquete.getCodigoQR()).thenReturn(codigo);
        when(tiquete.getEvento()).thenReturn(evento);
        when(tiquete.getLocalidad()).thenReturn(localidad);
        return tiquete;
    }
}
