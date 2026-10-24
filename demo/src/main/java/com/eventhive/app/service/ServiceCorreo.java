package com.eventhive.app.service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import com.eventhive.app.utils.GeneradorCodigoQR;

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
    private final GeneradorCodigoQR generadorCodigoQR;

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

        List<Tiquete> tiquetes = tiqueteRepository.findByCompraIdConDetalles(compra.getId());
        List<Boleto> boletos = tiquetes.stream()
                .map(this::aBoleto)
                .toList();
        Map<String, byte[]> imagenesEnLinea = new LinkedHashMap<>();
        for (int indice = 0; indice < tiquetes.size(); indice++) {
            imagenesEnLinea.put(CorreoPlantillas.cidQr(indice),
                    generadorCodigoQR.generarPng(tiquetes.get(indice).getCodigoQR()));
        }

        String html = plantillas.boletos(cliente.getNombreCompleto(), compra.getId(), boletos);
        sender.enviarAsync(cliente.getCorreo(), "Tus boletos — EventHive", html, imagenesEnLinea);
    }

    // INVITACION CON ROLES
    // Síncrono a propósito: si el correo falla, la invitación no debe quedar creada.
    public void enviarInvitacionRol(String correoDestino, String rol, String enlace) {
        enviarInvitacionRol(correoDestino, rol, null, enlace);
    }

    public void enviarInvitacionRol(String correoDestino, String rol, String organizacionNombre, String enlace) {
        String html = plantillas.invitacionRol(rol, organizacionNombre, enlace, 24);
        String asunto = (organizacionNombre != null && !organizacionNombre.isBlank())
                ? "Invitación para unirte a " + organizacionNombre + " en EventHive como " + rol
                : "Invitación para unirte a EventHive como " + rol;
        sender.enviar(correoDestino, asunto, html);
    }

    // CUENTA

    public void enviarRecuperarClave(Usuario usuario, String enlace, long minutosVigencia) {
        String html = plantillas.recuperarClave(usuario.getNombreCompleto(), enlace, minutosVigencia);
        sender.enviarAsync(usuario.getCorreo(), "Restablecer contraseña — EventHive", html);
    }

    // PROMOCIONES
    public void enviarPromocionConfirmada(com.eventhive.app.model.PosicionamientoEvento pago) {
        if (pago == null || pago.getOrganizador() == null) return;
        Usuario organizador = pago.getOrganizador();
        if (sinCorreo(organizador)) return;

        String html = plantillas.promocionConfirmada(
                organizador.getNombreCompleto(),
                pago.getEvento() != null ? pago.getEvento().getTitulo() : "",
                pago.getPlan() != null ? pago.getPlan().getNombre() : "Plan Promocional",
                pago.getPrecio() != null ? pago.getPrecio().toPlainString() : "0.00");

        sender.enviarAsync(organizador.getCorreo(), "Promoción confirmada — EventHive", html);
    }

    // CORREOS ADMINISTRATIVOS
    public void enviarCorreoAdministrativo(String destino, String asunto, String mensaje, Usuario admin) {
        if (admin == null || admin.getRol() == null || !"ADMINISTRADOR".equalsIgnoreCase(admin.getRol().getNombre())) {
            throw new com.eventhive.app.exception.BusinessException("Solo los administradores pueden enviar correos directos");
        }
        if (destino == null || destino.isBlank()) {
            throw new com.eventhive.app.exception.BusinessException("El correo destinatario es obligatorio");
        }
        if (asunto == null || asunto.isBlank()) {
            throw new com.eventhive.app.exception.BusinessException("El asunto es obligatorio");
        }
        if (mensaje == null || mensaje.isBlank()) {
            throw new com.eventhive.app.exception.BusinessException("El mensaje es obligatorio");
        }
        String remitenteInfo = admin.getNombreCompleto() + " (" + admin.getCorreo() + ")";
        String html = plantillas.correoAdministrativo(remitenteInfo, mensaje);
        // Envío síncrono para verificar aceptación del servidor de correo
        sender.enviar(destino.trim(), asunto.trim(), html);
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
