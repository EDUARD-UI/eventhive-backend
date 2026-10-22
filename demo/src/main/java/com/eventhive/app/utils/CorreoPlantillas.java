package com.eventhive.app.utils;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class CorreoPlantillas {

    public record ItemPago(String evento, String localidad, int cantidad, String subtotal) {}

    public record Boleto(String evento, String fecha, String hora, String lugar,
                         String localidad, String codigoQR) {}

    // ---------- Correos del sistema ----------

    public String pagoConfirmado(String nombre, Long compraId, String fecha,
                                 String total, String metodoPago, List<ItemPago> items) {
        StringBuilder filas = new StringBuilder();
        for (ItemPago i : items) {
            filas.append("<tr>")
                    .append(celda(esc(i.evento()), "left"))
                    .append(celda(esc(i.localidad()), "left"))
                    .append(celda(String.valueOf(i.cantidad()), "center"))
                    .append(celda("$" + esc(i.subtotal()), "right"))
                    .append("</tr>");
        }

        String contenido =
                "<table style=\"width:100%;border-collapse:collapse;margin-top:16px;\">"
                        + "<thead><tr style=\"background:#f5f5f7;\">"
                        + "<th style=\"padding:8px;text-align:left;\">Evento</th>"
                        + "<th style=\"padding:8px;text-align:left;\">Localidad</th>"
                        + "<th style=\"padding:8px;text-align:center;\">Cant.</th>"
                        + "<th style=\"padding:8px;text-align:right;\">Subtotal</th>"
                        + "</tr></thead><tbody>" + filas + "</tbody></table>"
                        + "<p style=\"text-align:right;font-size:16px;\"><strong>Total pagado: $" + esc(total) + "</strong></p>"
                        + "<p style=\"color:#666;font-size:13px;\">Compra #" + compraId
                        + " · " + esc(fecha) + " · Método de pago: " + esc(metodoPago) + "</p>";

        return base("¡Pago confirmado!",
                "Hola " + esc(nombre) + ",<br>Tu pago fue procesado con éxito. "
                        + "Los boletos te llegan en un correo aparte.",
                contenido);
    }

    public String boletos(String nombre, Long compraId, List<Boleto> boletos) {
        StringBuilder bloques = new StringBuilder();
        for (Boleto b : boletos) {
            bloques.append("<div style=\"border:1px solid #e0e0e6;border-radius:8px;padding:16px;margin-top:12px;\">")
                    .append("<h3 style=\"margin:0 0 4px;\">").append(esc(b.evento())).append("</h3>")
                    .append("<p style=\"margin:2px 0;color:#444;\">📅 ").append(esc(b.fecha()))
                    .append(" · 🕐 ").append(esc(b.hora())).append("</p>")
                    .append("<p style=\"margin:2px 0;color:#444;\">📍 ").append(esc(b.lugar())).append("</p>")
                    .append("<p style=\"margin:2px 0;color:#444;\">Localidad: ").append(esc(b.localidad())).append("</p>")
                    .append("<p style=\"margin:8px 0 0;font-family:monospace;font-size:14px;background:#f5f5f7;")
                    .append("padding:8px;border-radius:6px;\">Código: ").append(esc(b.codigoQR())).append("</p>")
                    .append("</div>");
        }

        return base("Tus boletos",
                "Hola " + esc(nombre) + ",<br>Estos son los boletos de tu compra #" + compraId + ".",
                bloques + "<p style=\"margin-top:16px;\">Presenta el código QR desde la app en el ingreso.</p>");
    }

    public String invitacionRol(String rol, String enlace, int horasVigencia) {
        return invitacionRol(rol, null, enlace, horasVigencia);
    }

    public String invitacionRol(String rol, String organizacionNombre, String enlace, int horasVigencia) {
        String mensajeRol;
        if (organizacionNombre != null && !organizacionNombre.isBlank()) {
            mensajeRol = "Recibiste una invitación para unirte a <strong>" + esc(organizacionNombre)
                    + "</strong> en EventHive con el rol <strong>" + esc(rol) + "</strong>.";
        } else {
            mensajeRol = "Recibiste una invitación para crear una cuenta EventHive con el rol <strong>"
                    + esc(rol) + "</strong>.";
        }
        return base("Completa tu registro",
                mensajeRol,
                "<p>El enlace es de un solo uso y vence en " + horasVigencia + " horas.</p>"
                        + boton("Aceptar invitación", enlace)
                        + "<p>Si no solicitaste esta invitación, puedes ignorar este correo.</p>");
    }

    public String recuperarClave(String nombre, String enlace, long minutosVigencia) {
        return base("Restablecer contraseña",
                "Hola " + esc(nombre) + ",<br>Recibimos una solicitud para restablecer tu contraseña.",
                boton("Crear nueva contraseña", enlace)
                        + "<p>Si no fuiste tú, ignora este correo: tu contraseña actual sigue siendo válida.</p>");
    }

    public String promocionConfirmada(String nombre, String eventoTitulo, String planNombre, String precio) {
        String contenido =
                "<div style=\"border:1px solid #e0e0e6;border-radius:8px;padding:16px;margin-top:12px;background:#fdfdff;\">"
                + "<p style=\"margin:4px 0;\"><strong>Evento:</strong> " + esc(eventoTitulo) + "</p>"
                + "<p style=\"margin:4px 0;\"><strong>Plan contratado:</strong> " + esc(planNombre) + "</p>"
                + "<p style=\"margin:4px 0;\"><strong>Importe pagado:</strong> $" + esc(precio) + " COP</p>"
                + "</div>"
                + "<p style=\"margin-top:16px;color:#444;\">Tu evento ya se encuentra registrado con promoción activa y estado Destacado.</p>";

        return base("¡Promoción confirmada!",
                "Hola " + esc(nombre) + ",<br>El pago del plan publicitario para tu evento ha sido procesado con éxito.",
                contenido);
    }

    public String correoAdministrativo(String remitenteAdmin, String mensajeContenido) {
        String contenido =
                "<div style=\"border:1px solid #e0e0e6;border-radius:8px;padding:16px;margin-top:12px;background:#fdfdff;\">"
                + "<p style=\"white-space:pre-wrap;line-height:1.6;color:#333;\">" + esc(mensajeContenido) + "</p>"
                + "</div>"
                + "<p style=\"color:#777;font-size:13px;margin-top:16px;\">Comunicación iniciada por el administrador: <strong>"
                + esc(remitenteAdmin) + "</strong></p>";

        return base("Comunicado administrativo oficial",
                "Has recibido un mensaje directo del equipo de administración de EventHive:",
                contenido);
    }

    // ---------- Piezas reutilizables ----------

    private String base(String titulo, String introduccion, String contenido) {
        return "<!DOCTYPE html><html><body style=\"font-family:Arial,sans-serif;background:#f0f0f3;padding:24px;\">"
                + "<div style=\"max-width:600px;margin:auto;background:#ffffff;border-radius:12px;padding:32px;\">"
                + "<h1 style=\"color:#5b2a86;margin-top:0;\">" + esc(titulo) + "</h1>"
                + "<p style=\"color:#333;font-size:15px;\">" + introduccion + "</p>"
                + contenido
                + "<p style=\"margin-top:32px;color:#999;font-size:12px;\">"
                + "Correo automático de EventHive, no respondas a este mensaje.</p>"
                + "</div></body></html>";
    }

    private String boton(String texto, String enlace) {
        return "<p><a href=\"" + esc(enlace) + "\" style=\"display:inline-block;background:#5b2a86;"
                + "color:#ffffff;padding:12px 20px;text-decoration:none;border-radius:6px;\">"
                + esc(texto) + "</a></p>";
    }

    private String celda(String valor, String alineacion) {
        return "<td style=\"padding:8px;border-bottom:1px solid #eee;text-align:" + alineacion + ";\">"
                + valor + "</td>";
    }

    private static String esc(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
