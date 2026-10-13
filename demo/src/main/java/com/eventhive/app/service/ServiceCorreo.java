package com.eventhive.app.service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.eventhive.app.model.Compra;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.Tiquete;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.TiqueteRepository;
import com.eventhive.app.utils.CorreoPlantillas;
import com.eventhive.app.utils.CorreoPlantillas.Boleto;
import com.eventhive.app.utils.CorreoPlantillas.ItemPago;
import com.eventhive.app.utils.CorreoSender;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceCorreo {

    private static final Locale ES = Locale.forLanguageTag("es-ES");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", ES);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("hh:mm a", ES);

    private final CorreoSender sender;
    private final CorreoPlantillas plantillas;
    private final TiqueteRepository tiqueteRepository;

    // COMPRAS

    public void enviarPagoConfirmado(Compra compra) {
        Usuario cliente = compra.getCliente();
        if (sinCorreo(cliente)) return;

        List<ItemPago> items = compra.getItems().stream()
                .map(i -> new ItemPago(
                        i.getEvento().getTitulo(),
                        i.getLocalidad().getNombre(),
                        i.getCantidad(),
                        i.getPrecioUnitario().multiply(BigDecimal.valueOf(i.getCantidad())).toPlainString()))
                .toList();

        String html = plantillas.pagoConfirmado(
                cliente.getNombreCompleto(), compra.getId(),
                compra.getFechaCompra().format(FECHA),
                compra.getTotal().toPlainString(), compra.getMetodoPago(), items);

        sender.enviarAsync(cliente.getCorreo(), "Pago confirmado — EventHive", html);
    }

    public void enviarBoletos(Compra compra) {
        Usuario cliente = compra.getCliente();
        if (sinCorreo(cliente)) return;

        List<Boleto> boletos = tiqueteRepository.findByCompraIdConDetalles(compra.getId()).stream()
                .map(this::aBoleto)
                .toList();

        String html = plantillas.boletos(cliente.getNombreCompleto(), compra.getId(), boletos);
        sender.enviarAsync(cliente.getCorreo(), "Tus boletos — EventHive", html);
    }

    // INVITACION CON ROLES

    // Síncrono a propósito: si el correo falla, la invitación no debe quedar creada.
    public void enviarInvitacionRol(String correoDestino, String rol, String enlace) {
        String html = plantillas.invitacionRol(rol, enlace, 24);
        sender.enviar(correoDestino, "Invitación para unirte a EventHive como " + rol, html);
    }

    // CUENTA

    public void enviarRecuperarClave(Usuario usuario, String enlace, long minutosVigencia) {
        String html = plantillas.recuperarClave(usuario.getNombreCompleto(), enlace, minutosVigencia);
        sender.enviarAsync(usuario.getCorreo(), "Restablecer contraseña — EventHive", html);
    }

    //METOODS AUXILIARES

    private Boleto aBoleto(Tiquete t) {
        Evento e = t.getEvento();
        return new Boleto(
                e.getTitulo(),
                e.getFecha() != null ? e.getFecha().format(FECHA) : "",
                e.getHora() != null ? e.getHora().format(HORA) : "",
                e.getLugar(),
                t.getLocalidad() != null ? t.getLocalidad().getNombre() : "",
                t.getCodigoQR());
    }

    private boolean sinCorreo(Usuario u) {
        return u == null || u.getCorreo() == null;
    }
}
