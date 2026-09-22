package com.eventhive.app.service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.eventhive.app.model.Compra;
import com.eventhive.app.model.Evento;
import com.eventhive.app.model.ItemCompra;
import com.eventhive.app.model.Usuario;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceCorreo {

    private final JavaMailSender mailSender;

    @Value("${eventhive.mail.remitente:no-reply@eventhive.local}")
    private String remitente;

    @Value("${eventhive.mail.nombre-remitente}")
    private String nombreRemitente;

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
    private static final DateTimeFormatter FORMATO_HORA =
            DateTimeFormatter.ofPattern("hh:mm a", new Locale("es", "ES"));

    // ===================== COMPRAS =====================

    @Async("emailExecutor")
    public void enviarConfirmacionCompra(Compra compra) {
        Usuario cliente = compra.getCliente();
        if (sinCorreo(cliente)) return;

        enviarSeguro(cliente.getCorreo(), compra.getId(), () -> {
            String asunto = "Tu compra fue confirmada — EventHive";
            String html = construirHtmlConfirmacionCompra(compra, cliente);
            enviar(cliente.getCorreo(), asunto, html);
        });
    }

    @Async("emailExecutor")
    public void enviarCancelacionCompra(Compra compra) {
        Usuario cliente = compra.getCliente();
        if (sinCorreo(cliente)) return;

        enviarSeguro(cliente.getCorreo(), compra.getId(), () -> {
            String asunto = "Tu compra fue cancelada — EventHive";
            String contenido = "<p>Número de compra: #" + compra.getId() + "</p>"
                    + "<p style=\"color:#666;font-size:13px;\">Si corresponde una devolución, "
                    + "el proceso se procesará según el método de pago original.</p>";
            String html = baseTemplate(
                    "Compra cancelada",
                    "Hola " + safeNombre(cliente) + ",<br>Tu compra fue cancelada correctamente.",
                    contenido);
            enviar(cliente.getCorreo(), asunto, html);
        });
    }

    // ===================== EVENTOS =====================

    @Async("emailExecutor")
    public void enviarRecordatorioEventoHoy(Evento evento, Usuario cliente) {
        if (sinCorreo(cliente)) return;

        enviarSeguro(cliente.getCorreo(), evento.getId(), () -> {
            String asunto = "¡Hoy es el evento! " + evento.getTitulo() + " — EventHive";
            String html = construirHtmlRecordatorioEvento(evento, cliente);
            enviar(cliente.getCorreo(), asunto, html);
        });
    }

    @Async("emailExecutor")
    public void enviarEventoCancelado(Evento evento, Usuario destinatario) {
        if (sinCorreo(destinatario)) return;

        enviarSeguro(destinatario.getCorreo(), evento.getId(), () -> {
            String asunto = "Evento cancelado: " + evento.getTitulo() + " — EventHive";
            String contenido = "<h2 style=\"margin:16px 0 4px;\">" + evento.getTitulo() + "</h2>"
                    + "<p>Revisa la información relacionada con tu compra en la app.</p>";
            String html = baseTemplate(
                    "Evento cancelado",
                    "Hola " + safeNombre(destinatario) + ",<br>El evento al que tenías boleto fue cancelado.",
                    contenido);
            enviar(destinatario.getCorreo(), asunto, html);
        });
    }

    @Async("emailExecutor")
    public void enviarResultadoModeracionEvento(String correoDestino, String nombreDestino,
                                                String tituloEvento, boolean aprobado, String motivo) {
        if (correoDestino == null) return;

        enviarSeguro(correoDestino, null, () -> {
            String titulo = aprobado ? "Evento aprobado" : "Evento rechazado";
            String asunto = titulo + ": " + tituloEvento + " — EventHive";
            String cuerpo = aprobado
                    ? "Tu evento <strong>" + tituloEvento + "</strong> fue aprobado y puede continuar con el proceso de publicación."
                    : "Tu evento <strong>" + tituloEvento + "</strong> fue rechazado."
                    + (motivo != null ? " Motivo: " + motivo : " Revisa las observaciones del moderador.");
            String html = baseTemplate(titulo,
                    "Hola " + safeNombreTexto(nombreDestino) + ",", "<p>" + cuerpo + "</p>");
            enviar(correoDestino, asunto, html);
        });
    }

    // ===================== ORGANIZACIÓN =====================

    @Async("emailExecutor")
    public void enviarInvitacionOrganizacion(String correoDestino, String nombreDestino, String organizacionNombre) {
        if (correoDestino == null) return;

        enviarSeguro(correoDestino, null, () -> {
            String asunto = "Invitación a " + organizacionNombre + " — EventHive";
            String contenido = "<p>Has recibido una invitación para formar parte de "
                    + "<strong>" + organizacionNombre + "</strong>. Ingresa a la app para aceptarla o rechazarla.</p>";
            String html = baseTemplate("Invitación recibida",
                    "Hola " + safeNombreTexto(nombreDestino) + ",", contenido);
            enviar(correoDestino, asunto, html);
        });
    }

    @Async("emailExecutor")
    public void enviarRolRevocado(String correoDestino, String nombreDestino, String detalle) {
        if (correoDestino == null) return;

        enviarSeguro(correoDestino, null, () -> {
            String asunto = "Cambio de permisos — EventHive";
            String html = baseTemplate("Rol revocado",
                    "Hola " + safeNombreTexto(nombreDestino) + ",",
                    "<p>" + detalle + "</p>");
            enviar(correoDestino, asunto, html);
        });
    }

    @Async("emailExecutor")
    public void enviarResultadoSolicitudOrganizacion(String correoDestino, String nombreDestino,
                                                     boolean aprobada, String motivo) {
        if (correoDestino == null) return;

        enviarSeguro(correoDestino, null, () -> {
            String titulo = aprobada ? "Solicitud aprobada" : "Solicitud rechazada";
            String asunto = titulo + " — EventHive";
            String cuerpo = aprobada
                    ? "Tu solicitud de organización fue aprobada. Ya puedes empezar a publicar eventos."
                    : "Tu solicitud de organización fue rechazada."
                    + (motivo != null ? " Motivo: " + motivo : "");
            String html = baseTemplate(titulo,
                    "Hola " + safeNombreTexto(nombreDestino) + ",", "<p>" + cuerpo + "</p>");
            enviar(correoDestino, asunto, html);
        });
    }

    // ===================== ENVÍO BASE =====================

    private void enviar(String destinatario, String asunto, String htmlBody) throws Exception {
        MimeMessage mensaje = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensaje, "UTF-8");
        helper.setFrom(remitente, nombreRemitente);
        helper.setTo(destinatario);
        helper.setSubject(asunto);
        helper.setText(htmlBody, true);
        mailSender.send(mensaje);
    }

    private interface EnvioMail {
        void ejecutar() throws Exception;
    }

    private void enviarSeguro(String destino, Object refId, EnvioMail accion) {
        try {
            accion.ejecutar();
            log.info("Correo enviado a {} (ref={})", destino, refId);
        } catch (Exception e) {
            log.error("Error al enviar correo a {} (ref={}): {}", destino, refId, e.getMessage());
        }
    }

    private boolean sinCorreo(Usuario usuario) {
        return usuario == null || usuario.getCorreo() == null;
    }

    // ===================== PLANTILLAS =====================

    private String construirHtmlConfirmacionCompra(Compra compra, Usuario cliente) {
        StringBuilder filas = new StringBuilder();
        for (ItemCompra item : compra.getItems()) {
            BigDecimal subtotal = item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad()));
            filas.append("<tr>")
                    .append("<td style=\"padding:8px;border-bottom:1px solid #eee;\">")
                    .append(item.getEvento().getTitulo()).append("</td>")
                    .append("<td style=\"padding:8px;border-bottom:1px solid #eee;\">")
                    .append(item.getLocalidad().getNombre()).append("</td>")
                    .append("<td style=\"padding:8px;border-bottom:1px solid #eee;text-align:center;\">")
                    .append(item.getCantidad()).append("</td>")
                    .append("<td style=\"padding:8px;border-bottom:1px solid #eee;text-align:right;\">$")
                    .append(subtotal).append("</td>")
                    .append("</tr>");
        }

        return baseTemplate(
                "¡Pago confirmado!",
                "Hola " + safeNombre(cliente) + ",<br>Tu compra fue procesada con éxito y tus tiquetes ya están disponibles en tu cuenta de EventHive.",
                "<table style=\"width:100%;border-collapse:collapse;margin-top:16px;\">"
                        + "<thead><tr style=\"background:#f5f5f7;text-align:left;\">"
                        + "<th style=\"padding:8px;\">Evento</th><th style=\"padding:8px;\">Localidad</th>"
                        + "<th style=\"padding:8px;text-align:center;\">Cant.</th><th style=\"padding:8px;text-align:right;\">Subtotal</th>"
                        + "</tr></thead><tbody>" + filas + "</tbody></table>"
                        + "<p style=\"text-align:right;font-size:16px;margin-top:12px;\"><strong>Total pagado: $"
                        + compra.getTotal() + "</strong></p>"
                        + "<p style=\"color:#666;font-size:13px;\">Número de compra: #" + compra.getId()
                        + " · Método de pago: " + compra.getMetodoPago() + "</p>"
        );
    }

    private String construirHtmlRecordatorioEvento(Evento evento, Usuario cliente) {
        String fecha = evento.getFecha() != null ? evento.getFecha().format(FORMATO_FECHA) : "";
        String hora = evento.getHora() != null ? evento.getHora().format(FORMATO_HORA) : "";

        return baseTemplate(
                "¡Hoy es el gran día!",
                "Hola " + safeNombre(cliente) + ",<br>Te recordamos que hoy asistes a:",
                "<h2 style=\"margin:16px 0 4px;\">" + evento.getTitulo() + "</h2>"
                        + "<p style=\"margin:4px 0;color:#444;\">📅 " + fecha + " · 🕐 " + hora + "</p>"
                        + (evento.getLugar() != null ? "<p style=\"margin:4px 0;color:#444;\">📍 " + evento.getLugar() + "</p>" : "")
                        + "<p style=\"margin-top:16px;\">No olvides llevar tu tiquete (código QR) desde la app o tu correo de confirmación de compra.</p>"
        );
    }

    private String baseTemplate(String titulo, String introduccion, String contenido) {
        return "<!DOCTYPE html>"
                + "<html><body style=\"font-family:Arial,sans-serif;background:#f0f0f3;padding:24px;\">"
                + "<div style=\"max-width:600px;margin:auto;background:#ffffff;border-radius:12px;padding:32px;\">"
                + "<h1 style=\"color:#5b2a86;margin-top:0;\">" + titulo + "</h1>"
                + "<p style=\"color:#333;font-size:15px;\">" + introduccion + "</p>"
                + contenido
                + "<p style=\"margin-top:32px;color:#999;font-size:12px;\">Este es un correo automático de EventHive, no respondas a este mensaje.</p>"
                + "</div></body></html>";
    }

    private String safeNombre(Usuario usuario) {
        return usuario.getNombreCompleto() != null ? usuario.getNombreCompleto() : "";
    }

    private String safeNombreTexto(String nombre) {
        return nombre != null ? nombre : "";
    }
}
